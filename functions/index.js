const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const bodyParser = require("body-parser");
const axios = require("axios");
const { Timestamp } = require("firebase-admin/firestore");
const { v4: uuidv4 } = require("uuid");

admin.initializeApp();

const IDENTITY_BASE_URL = "https://identitytoolkit.googleapis.com/v1";
const WEB_API_KEY = process.env.IDENTITY_WEB_API_KEY;

function buildIdentityToolkitUrl(resource) {
  if (!WEB_API_KEY) {
    throw new Error("IDENTITY_WEB_API_KEY env var not set");
  }
  return `${IDENTITY_BASE_URL}${resource}?key=${WEB_API_KEY}`;
}

// Only use emulators if explicitly set (for local development)
// In production, these will be undefined and Firebase will use the real services
if (!process.env.FIREBASE_AUTH_EMULATOR_HOST) {
  delete process.env.FIREBASE_AUTH_EMULATOR_HOST;
}
if (!process.env.FIRESTORE_EMULATOR_HOST) {
  delete process.env.FIRESTORE_EMULATOR_HOST;
}

const db = admin.firestore();
const app = express();
app.use(bodyParser.json());

// ===========================
// HELPERS
// ===========================

async function getUserDocByUid(uid) {
  const doc = await db.collection("users").doc(uid).get();
  return doc.exists ? doc.data() : null;
}

async function verifyToken(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith("Bearer ")) {
    return res
      .status(401)
      .json({ error: "Missing or invalid Authorization header" });
  }

  const idToken = auth.split("Bearer ")[1];
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    req.uid = decoded.uid;
    req.userDoc = await getUserDocByUid(req.uid);
    req.role = req.userDoc?.role || null;
    next();
  } catch (err) {
    console.error("Token verification failed:", err);
    return res.status(401).json({ error: "Invalid token" });
  }
}

function buildVitalsDocumentId(patientId, timestamp) {
  return `${patientId}_${timestamp}`;
}

function normalizeHealthConnectPayload(payload) {
  const {
    timestamp,
    patientId,
    heartRate = null,
    heartRateVariability = null,
    spo2 = null,
    oxygenSaturation = null,
    stressLevel = null,
    bodyBattery = null,
    deviceSource = "HealthConnect",
  } = payload || {};

  if (!patientId || typeof timestamp !== "number") {
    return null;
  }

  return {
    patientId,
    timestamp,
    heartRate,
    heartRateVariability,
    spo2: spo2 ?? oxygenSaturation,
    stressLevel,
    bodyBattery,
    deviceSource,
  };
}

async function upsertVitalsSnapshot(vital) {
  const docId = buildVitalsDocumentId(vital.patientId, vital.timestamp);
  const docRef = db.collection("vital_readings").doc(docId);
  await docRef.set({ id: docId, ...vital }, { merge: true });
  return docId;
}

// ===========================
// AUTH ENDPOINTS
// ===========================

// POST /auth/register
// body: { email, password, name, role }
app.post("/auth/register", async (req, res) => {
  try {
    const { email, password, name, role } = req.body;

    if (!email || !password || !name || !role) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    if (!["Nurse", "Patient"].includes(role)) {
      return res
        .status(400)
        .json({ error: "role must be 'Nurse' or 'Patient'" });
    }

    // Criar user no Firebase Auth
    const userRecord = await admin.auth().createUser({
      email,
      password,
      displayName: name,
    });

    const uid = userRecord.uid;

    // Criar documento Firestore com modelo N:N
    let userData = {
      id: uid,
      name,
      role,
      email,
      createdAt: Timestamp.now(),
    };

    if (role === "Nurse") {
      userData.patientIds = [];
    } else if (role === "Patient") {
      userData.nurseIds = [];
      userData.usesHealthConnect = false; // flag útil do lado da app
    }

    await db.collection("users").doc(uid).set(userData);

    // Obter ID Token via Firebase Auth
    let idToken;
    if (process.env.FIREBASE_AUTH_EMULATOR_HOST) {
      // Local emulator
      const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
      const signInUrl = `http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key`;
      const resp = await axios.post(signInUrl, {
        email,
        password,
        returnSecureToken: true
      });
      idToken = resp.data.idToken;
    } else {
      // Production: Use Firebase Auth REST API
      // Get API key from Firebase config or environment variable
      const apiKey = functions.config().firebase?.apiKey || process.env.FIREBASE_API_KEY;
      if (!apiKey) {
        // Fallback: Create custom token (client will need to exchange it)
        idToken = await admin.auth().createCustomToken(uid);
      } else {
        const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`;
        const resp = await axios.post(signInUrl, {
          email,
          password,
          returnSecureToken: true
        });
        idToken = resp.data.idToken;
      }
    }

    return res.json({ idToken, user: userData });
  } catch (error) {
    console.error("Register error:", error?.response?.data || error);
    if (error.code === "auth/email-already-exists") {
      return res.status(400).json({ error: "Email already in use" });
    }
    return res
      .status(500)
      .json({ error: "Internal server error", details: error?.message });
  }
});

// POST /auth/login
// body: { email, password }
app.post("/auth/login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    let idToken;
    let uid;
    
    if (process.env.FIREBASE_AUTH_EMULATOR_HOST) {
      // Local emulator
      const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
      const signInUrl = `http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key`;
      const resp = await axios.post(signInUrl, {
        email,
        password,
        returnSecureToken: true
      });
      idToken = resp.data.idToken;
      uid = resp.data.localId;
    } else {
      // Production: Use Firebase Auth REST API
      const apiKey = functions.config().firebase?.apiKey || process.env.FIREBASE_API_KEY;
      if (!apiKey) {
        return res.status(500).json({ error: "Firebase API key not configured. Please set FIREBASE_API_KEY environment variable." });
      }
      
      try {
        const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`;
        const resp = await axios.post(signInUrl, {
          email,
          password,
          returnSecureToken: true
        });
        idToken = resp.data.idToken;
        uid = resp.data.localId;
      } catch (error) {
        const errData = error?.response?.data;
        if (errData && errData.error) {
          const errorMsg = errData.error.message || "Invalid email or password";
          return res.status(400).json({ error: errorMsg });
        }
        throw error;
      }
    }

    const userDoc = await getUserDocByUid(uid);
    if (!userDoc) {
      return res.status(404).json({ error: "User doc not found" });
    }

    return res.json({ idToken, user: userDoc });
  } catch (error) {
    console.error("Login error:", error?.response?.data || error);
    const errData = error?.response?.data;
    if (errData && errData.error && errData.error.message) {
      return res.status(400).json({ error: errData.error.message });
    }
    return res
      .status(500)
      .json({ error: "Internal server error", details: error?.message });
  }
});

// ===========================
// PROTECTED ENDPOINTS
// ===========================

// GET /patients/:id
app.get("/patients/:id", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    const patientDocSnapshot = await db
      .collection("users")
      .doc(patientId)
      .get();
    if (!patientDocSnapshot.exists) {
      return res.status(404).json({ error: "Patient not found" });
    }
    const patient = patientDocSnapshot.data();

    // próprio paciente
    if (requesterUid === patientId) {
      return res.json({ patient });
    }

    // nurse que tenha este paciente associado
    if (requesterRole === "Nurse") {
      const nurseDocSnapshot = await db
        .collection("users")
        .doc(requesterUid)
        .get();
      const nurseDoc = nurseDocSnapshot.data();
      const nursePatientIds = nurseDoc?.patientIds || [];
      if (Array.isArray(nursePatientIds) && nursePatientIds.includes(patientId)) {
        return res.json({ patient });
      } else {
        return res
          .status(403)
          .json({ error: "Not authorized to view this patient" });
      }
    }

    return res.status(403).json({ error: "Not authorized" });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// GET /nurses/:id/patients
app.get("/nurses/:id/patients", verifyToken, async (req, res) => {
  try {
    const nurseId = req.params.id;
    const requesterUid = req.uid;

    if (req.role !== "Nurse") {
      return res
        .status(403)
        .json({ error: "Only nurses can access this endpoint" });
    }
    if (requesterUid !== nurseId) {
      return res
        .status(403)
        .json({ error: "You can only fetch your own patients" });
    }

    const nurseDoc = await db.collection("users").doc(nurseId).get();
    if (!nurseDoc.exists) {
      return res.status(404).json({ error: "Nurse not found" });
    }

    const nurseData = nurseDoc.data();
    const patientIds = nurseData.patientIds || [];

    if (!Array.isArray(patientIds) || patientIds.length === 0) {
      return res.json({ patients: [] });
    }

    const patientPromises = patientIds.map((pid) =>
      db.collection("users").doc(pid).get()
    );
    const snaps = await Promise.all(patientPromises);
    const patients = snaps.filter((s) => s.exists).map((s) => s.data());

    return res.json({ patients });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// VITALS
// ===========================

// POST /patients/:id/vitals
// body: { heartRate, spo2, stressLevel, bodyBattery?, deviceSource }
app.post("/patients/:id/vitals", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterUid !== patientId && requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Not authorized" });
    }

    // se for nurse, tem de estar associado a este paciente
    if (requesterRole === "Nurse" && requesterUid !== patientId) {
      const nurseDoc = await db.collection("users").doc(requesterUid).get();
      const nursePatientIds = nurseDoc.data()?.patientIds || [];
      if (!nursePatientIds.includes(patientId)) {
        return res
          .status(403)
          .json({ error: "Nurse not authorized for this patient" });
      }
    }

    const { heartRate, spo2, stressLevel, bodyBattery = null, deviceSource } =
      req.body;

    if (
      heartRate == null ||
      spo2 == null ||
      stressLevel == null ||
      !deviceSource
    ) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    const vitalData = {
      patientId,
      timestamp: Date.now(),
      heartRate,
      spo2,
      stressLevel,
      bodyBattery,
      deviceSource,
    };

    const docRef = await db.collection("vital_readings").add(vitalData);
    await docRef.update({ id: docRef.id });

    return res.json({
      message: "Vital reading saved",
      vital: { id: docRef.id, ...vitalData },
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /patients/:id/vitals/batch
app.post("/patients/:id/vitals/batch", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterUid !== patientId && requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Not authorized" });
    }

    if (requesterRole === "Nurse" && requesterUid !== patientId) {
      const nurseDoc = await db.collection("users").doc(requesterUid).get();
      const nursePatientIds = nurseDoc.data()?.patientIds || [];
      if (!nursePatientIds.includes(patientId)) {
        return res.status(403).json({ error: "Nurse not authorized for this patient" });
      }
    }

    const vitalsArray = Array.isArray(req.body?.vitals) ? req.body.vitals : [];
    if (vitalsArray.length === 0) {
      return res.status(400).json({ error: "vitals array is required" });
    }

    const normalized = vitalsArray
      .map((entry) => normalizeHealthConnectPayload({ ...entry, patientId }))
      .filter((item) => item);

    if (normalized.length === 0) {
      return res.status(400).json({ error: "No valid vitals payload" });
    }

    await Promise.all(normalized.map((vital) => upsertVitalsSnapshot(vital)));

    return res.json({ message: "Vitals batch processed", saved: normalized.length });
  } catch (err) {
    console.error("/patients/:id/vitals/batch failed", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// GET /patients/:id/vitals?from=TIMESTAMP&to=TIMESTAMP
app.get("/patients/:id/vitals", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterUid !== patientId && requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Not authorized" });
    }

    if (requesterRole === "Nurse" && requesterUid !== patientId) {
      const nurseDoc = await db.collection("users").doc(requesterUid).get();
      const nursePatientIds = nurseDoc.data()?.patientIds || [];
      if (!nursePatientIds.includes(patientId)) {
        return res
          .status(403)
          .json({ error: "Nurse not authorized for this patient" });
      }
    }

    let fromTs = parseInt(req.query.from) || 0;
    let toTs = parseInt(req.query.to) || Date.now();

    const querySnapshot = await db
      .collection("vital_readings")
      .where("patientId", "==", patientId)
      .where("timestamp", ">=", fromTs)
      .where("timestamp", "<=", toTs)
      .orderBy("timestamp", "asc")
      .get();

    const vitals = querySnapshot.docs.map((doc) => doc.data());
    return res.json({ vitals });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// ALERTS
// ===========================

// POST /patients/:id/alerts
// body: { severity, message }
app.post("/patients/:id/alerts", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterUid !== patientId && requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Not authorized" });
    }

    if (requesterRole === "Nurse" && requesterUid !== patientId) {
      const nurseDoc = await db.collection("users").doc(requesterUid).get();
      const nursePatientIds = nurseDoc.data()?.patientIds || [];
      if (!nursePatientIds.includes(patientId)) {
        return res
          .status(403)
          .json({ error: "Nurse not authorized for this patient" });
      }
    }

    const { severity, message } = req.body;
    if (severity === undefined || !message) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    const alertData = {
      patientId,
      timestamp: Date.now(),
      severity,
      message,
      acknowledged: false,
    };

    const docRef = await db.collection("stress_alerts").add(alertData);
    await docRef.update({ id: docRef.id });

    await fanOutStressAlert({
      alert: { id: docRef.id, ...alertData },
      patientId,
    });

    return res.json({
      message: "Alert created",
      alert: { id: docRef.id, ...alertData },
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

async function fanOutStressAlert({ alert, patientId }) {
  try {
    const patientDoc = await db.collection("users").doc(patientId).get();
    if (!patientDoc.exists) {
      console.warn(`Patient ${patientId} not found when fanning out alerts.`);
      return;
    }
    const patientData = patientDoc.data();
    const nurseIds = patientData.nurseIds || [];
    if (!Array.isArray(nurseIds) || nurseIds.length === 0) {
      return;
    }

    const nurseDocs = await Promise.all(
      nurseIds.map((nurseId) => db.collection("users").doc(nurseId).get())
    );

    const tokens = nurseDocs
      .map((doc) => doc.data()?.fcmTokens || [])
      .flat()
      .filter((token) => typeof token === "string" && token.length > 0);

    if (tokens.length === 0) {
      console.warn("No nurse tokens available for alert fan-out");
      return;
    }

    const message = {
      notification: {
        title: `${patientData.name || "Paciente"} stress elevado`,
        body: alert.message,
      },
      data: {
        patientId,
        alertId: alert.id,
        severity: String(alert.severity),
        timestamp: String(alert.timestamp),
        type: "STRESS_ALERT",
      },
      tokens,
    };

    const response = await admin.messaging().sendEachForMulticast(message);

    if (response.failureCount > 0) {
      const failedTokens = [];
      response.responses.forEach((resp, index) => {
        if (!resp.success) {
          failedTokens.push(tokens[index]);
        }
      });
      console.warn("Failed to deliver messages to:", failedTokens);
    }
  } catch (error) {
    console.error("fanOutStressAlert failed", error);
  }
}

// ===========================
// PILL IDENTIFY (DEMO)
// ===========================

// POST /pill/identify
// body: { imprint, color, shape, image? }
app.post("/pill/identify", async (req, res) => {
  try {
    const { imprint, color, shape, image } = req.body;

    if (!imprint && !color && !shape && !image) {
      return res.status(400).json({
        error:
          "At least one identifier (imprint, color, shape, or image) is required",
      });
    }

    const detectedImprint = imprint || "N/A";
    const detectedColor = color || "Unknown";
    const detectedShape = shape || "Unknown";

    const candidateMedications = [
      { name: "Aspirin", dosage: "500mg" },
      { name: "Paracetamol", dosage: "500mg" },
    ];

    const result = {
      imageLocalPath: image || null,
      detectedImprint,
      detectedColor,
      detectedShape,
      candidateMedications,
    };

    return res.json(result);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// QR SESSION (Paciente cria, Nurse lê)
// ===========================

// POST /patient/session
// Paciente cria sessão para ser lida por um enfermeiro (QR)
app.post("/patient/session", verifyToken, async (req, res) => {
  try {
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterRole !== "Patient") {
      return res
        .status(403)
        .json({ error: "Only patients can create a QR session" });
    }

    const qrToken = uuidv4();
    const expiresAt = Date.now() + 60 * 60 * 1000; // 1h

    await db.collection("qr_sessions").doc(qrToken).set({
      patientId: requesterUid,
      createdAt: Date.now(),
      expiresAt,
      resolved: false,
    });

    // qrToken é o valor que vais meter no QR code
    return res.json({ qrToken, expiresAt });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /patient/session/resolve
// body: { qrToken }
// Nurse lê o QR do paciente e liga-se a esse paciente
app.post("/patient/session/resolve", verifyToken, async (req, res) => {
  try {
    const requesterUid = req.uid; // nurse
    const requesterRole = req.role;
    const { qrToken } = req.body;

    if (!qrToken) {
      return res.status(400).json({ error: "Missing qrToken" });
    }

    if (requesterRole !== "Nurse") {
      return res
        .status(403)
        .json({ error: "Only nurses can resolve a QR session" });
    }

    const sessionRef = db.collection("qr_sessions").doc(qrToken);
    const sessionDoc = await sessionRef.get();

    if (!sessionDoc.exists) {
      return res.status(404).json({ error: "QR session not found" });
    }

    const session = sessionDoc.data();

    if (session.resolved) {
      return res.status(400).json({ error: "QR session already resolved" });
    }

    if (Date.now() > session.expiresAt) {
      return res.status(400).json({ error: "QR session expired" });
    }

    const patientId = session.patientId;
    const nurseId = requesterUid;

    // 1) marcar sessão como resolvida
    await sessionRef.update({
      resolved: true,
      nurseId,
      resolvedAt: Date.now(),
    });

    // 2) atualizar relação N:N
    await db
      .collection("users")
      .doc(nurseId)
      .update({
        patientIds: admin.firestore.FieldValue.arrayUnion(patientId),
      });

    await db
      .collection("users")
      .doc(patientId)
      .set(
        {
          nurseIds: admin.firestore.FieldValue.arrayUnion(nurseId),
        },
        { merge: true }
      );

    // 3) devolver dados do paciente
    const patientDoc = await db.collection("users").doc(patientId).get();
    if (!patientDoc.exists) {
      return res.status(404).json({ error: "Patient not found" });
    }

    return res.json({ patient: patientDoc.data() });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// PATIENT SEARCH
// ===========================

// POST /patients/search
// body: { email }
app.post("/patients/search", verifyToken, async (req, res) => {
  try {
    const { email } = req.body;
    const requesterRole = req.role;

    if (requesterRole !== "Nurse") {
      return res
        .status(403)
        .json({ error: "Only nurses can search patients" });
    }

    if (!email) {
      return res.status(400).json({ error: "Email is required" });
    }

    const userSnapshot = await db
      .collection("users")
      .where("email", "==", email)
      .where("role", "==", "Patient")
      .limit(1)
      .get();

    if (userSnapshot.empty) {
      return res.json({ found: false });
    }

    const patientDoc = userSnapshot.docs[0];
    const patientId = patientDoc.id;
    const patient = patientDoc.data();

    const vitalSnapshot = await db
      .collection("vital_readings")
      .where("patientId", "==", patientId)
      .orderBy("timestamp", "desc")
      .limit(1)
      .get();

    let latestVital = null;
    if (!vitalSnapshot.empty) {
      const doc = vitalSnapshot.docs[0];
      const data = doc.data();
      latestVital = {
        id: doc.id,
        timestamp: data.timestamp,
        heartRate: data.heartRate,
        spo2: data.spo2,
        stressLevel: data.stressLevel,
        bodyBattery: data.bodyBattery ?? null,
        deviceSource: data.deviceSource,
      };
    }

    console.log("Paciente encontrado:", patientId);
    console.log("Último vital:", latestVital);

    return res.json({
      found: true,
      patient: {
        id: patientId,
        name: patient.name,
        email: patient.email,
      },
      latestVital,
    });
  } catch (err) {
    console.error("Erro em /patients/search:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// MESSAGING
// ===========================

// POST /messaging/token
app.post("/messaging/token", verifyToken, async (req, res) => {
  try {
    const { token } = req.body;
    if (!token) {
      return res.status(400).json({ error: "Missing token" });
    }

    await db
      .collection("users")
      .doc(req.uid)
      .set(
        {
          fcmTokens: admin.firestore.FieldValue.arrayUnion(token),
          updatedAt: Date.now(),
        },
        { merge: true }
      );

    return res.json({ success: true });
  } catch (error) {
    console.error("/messaging/token failed", error);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// EXPORTAR APP
// ===========================
exports.api = functions.https.onRequest(app);
