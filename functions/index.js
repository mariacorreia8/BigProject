const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const bodyParser = require("body-parser");
const axios = require("axios");
const { Timestamp } = require("firebase-admin/firestore");
const { v4: uuidv4 } = require("uuid");



admin.initializeApp();

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


async function getUserDocByUid(uid) {
  const doc = await db.collection("users").doc(uid).get();
  return doc.exists ? doc.data() : null;
}


async function verifyToken(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Missing or invalid Authorization header" });
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

// ===========================
// AUTH ENDPOINTS
// ===========================

// POST /auth/register
//{"email": "joaquim", "password": "joaquim123", "name": "joaquim", "role": "Nurse"}
app.post("/auth/register", async (req, res) => {
  try {
    const { email, password, name, role, linkedGarminDeviceId = null, patientIds = [] } = req.body;

    if (!email || !password || !name || !role) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    if (!["Nurse", "Patient"].includes(role)) {
      return res.status(400).json({ error: "role must be 'Nurse' or 'Patient'" });
    }

    // Criar user no Firebase Auth
    const userRecord = await admin.auth().createUser({
      email,
      password,
      displayName: name,
    });

    const uid = userRecord.uid;

    // Criar documento Firestore
    const userData = {
      id: uid,
      name,
      role,
      email,
      linkedGarminDeviceId: linkedGarminDeviceId || null,
      patientIds: Array.isArray(patientIds) ? patientIds : [],
      createdAt: Timestamp.now(),
    };

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
    return res.status(500).json({ error: "Internal server error", details: error?.message });
  }
});

// POST /auth/login
app.post("/auth/login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ error: "Missing email or password" });

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
    if (!userDoc) return res.status(404).json({ error: "User doc not found" });

    return res.json({ idToken, user: userDoc });
  } catch (error) {
    console.error("Login error:", error?.response?.data || error);
    const errData = error?.response?.data;
    if (errData && errData.error && errData.error.message) {
      return res.status(400).json({ error: errData.error.message });
    }
    return res.status(500).json({ error: "Internal server error", details: error?.message });
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

    const patientDocSnapshot = await db.collection("users").doc(patientId).get();
    if (!patientDocSnapshot.exists) return res.status(404).json({ error: "Patient not found" });
    const patient = patientDocSnapshot.data();

    if (requesterUid === patientId) return res.json({ patient });

    if (requesterRole === "Nurse") {
      const nurseDocSnapshot = await db.collection("users").doc(requesterUid).get();
      const nurseDoc = nurseDocSnapshot.data();
      const nursePatientIds = nurseDoc?.patientIds || [];
      if (Array.isArray(nursePatientIds) && nursePatientIds.includes(patientId)) {
        return res.json({ patient });
      } else {
        return res.status(403).json({ error: "Not authorized to view this patient" });
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

    if (req.role !== "Nurse") return res.status(403).json({ error: "Only nurses can access this endpoint" });
    if (requesterUid !== nurseId) return res.status(403).json({ error: "You can only fetch your own patients" });

    const nurseDoc = await db.collection("users").doc(nurseId).get();
    if (!nurseDoc.exists) return res.status(404).json({ error: "Nurse not found" });

    const nurseData = nurseDoc.data();
    const patientIds = nurseData.patientIds || [];

    if (!Array.isArray(patientIds) || patientIds.length === 0) return res.json({ patients: [] });

    const patientPromises = patientIds.map(pid => db.collection("users").doc(pid).get());
    const snaps = await Promise.all(patientPromises);
    const patients = snaps.filter(s => s.exists).map(s => s.data());

    return res.json({ patients });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});




// POST /patients/:id/vitals
//body{"heartRate": 45, "spo2": 78, "stressLevel": 89, "deviceSource": "device_1"}
app.post("/patients/:id/vitals", verifyToken, async (req, res) => {
  try {
    const patientId = req.params.id;
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterUid !== patientId && requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Not authorized" });
    }

    /*if (requesterRole === "Nurse" && requesterUid !== patientId) {
      const nurseDoc = await db.collection("users").doc(requesterUid).get();
      const nursePatientIds = nurseDoc.data()?.patientIds || [];
      if (!nursePatientIds.includes(patientId)) {
        return res.status(403).json({ error: "Nurse not authorized for this patient" });
      }
    }*/

    const { heartRate, spo2, stressLevel, bodyBattery = null, deviceSource } = req.body;
    if (!heartRate || !spo2 || !stressLevel || !deviceSource) {
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

    return res.json({ message: "Vital reading saved", vital: { id: docRef.id, ...vitalData } });
  } catch (err) {
    console.error(err);
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
        return res.status(403).json({ error: "Nurse not authorized for this patient" });
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

    const vitals = querySnapshot.docs.map(doc => doc.data());
    return res.json({ vitals });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});


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
        return res.status(403).json({ error: "Nurse not authorized for this patient" });
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
      acknowledged: false
    };

    const docRef = await db.collection("stress_alerts").add(alertData);
    await docRef.update({ id: docRef.id });

    return res.json({ message: "Alert created", alert: { id: docRef.id, ...alertData } });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});


// GET /patients/:id/alerts
app.get("/patients/:id/alerts", verifyToken, async (req, res) => {
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

    const querySnapshot = await db
      .collection("stress_alerts")
      .where("patientId", "==", patientId)
      .orderBy("timestamp", "desc")
      .get();

    const alerts = querySnapshot.docs.map(doc => doc.data());
    return res.json({ alerts });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /pill/identify
// body: { imprint, color, shape, image? }
app.post("/pill/identify", async (req, res) => {
  try {
    const { imprint, color, shape, image } = req.body;

    if (!imprint && !color && !shape && !image) {
      return res.status(400).json({ error: "At least one identifier (imprint, color, shape, or image) is required" });
    }

    const detectedImprint = imprint || "N/A";
    const detectedColor = color || "Unknown";
    const detectedShape = shape || "Unknown";

    const candidateMedications = [
      { name: "Aspirin", dosage: "500mg" },
      { name: "Paracetamol", dosage: "500mg" }
    ];

    const result = {
      imageLocalPath: image || null,  
      detectedImprint,
      detectedColor,
      detectedShape,
      candidateMedications
    };

    return res.json(result);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /nurse/session
app.post("/nurse/session", verifyToken, async (req, res) => {
  try {
    const requesterUid = req.uid;
    const requesterRole = req.role;

    if (requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Only nurses can create a QR session" });
    }

    const qrToken = uuidv4();
    const expiresAt = Date.now() + 60 * 60 * 1000; 

    await db.collection("qr_sessions").doc(qrToken).set({
      nurseId: requesterUid,
      createdAt: Date.now(),
      expiresAt,
      resolved: false,
    });

    return res.json({ qrToken, expiresAt });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /nurse/session/resolve
// body: { qrToken, patientId }
app.post("/nurse/session/resolve", verifyToken, async (req, res) => {
  try {
    const requesterUid = req.uid;
    const requesterRole = req.role;
    const { qrToken, patientId } = req.body;

    if (!qrToken || !patientId) {
      return res.status(400).json({ error: "Missing qrToken or patientId" });
    }

    if (requesterRole !== "Patient") {
      return res.status(403).json({ error: "Only patients can resolve a QR session" });
    }

    const sessionDoc = await db.collection("qr_sessions").doc(qrToken).get();
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

    await db.collection("qr_sessions").doc(qrToken).update({
      resolved: true,
      patientId,
      resolvedAt: Date.now(),
    });

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

// POST /patients/search
app.post("/patients/search", verifyToken, async (req, res) => {
  try {
    const { email } = req.body;
    const requesterRole = req.role;

    if (requesterRole !== "Nurse") {
      return res.status(403).json({ error: "Only nurses can search patients" });
    }

    if (!email) {
      return res.status(400).json({ error: "Email is required" });
    }

    const userSnapshot = await db.collection("users")
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

    const vitalSnapshot = await db.collection("vital_readings")
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
        deviceSource: data.deviceSource
      };
    }

    console.log("Paciente encontrado:", patientId);
    console.log("Último vital:", latestVital);

    return res.json({
      found: true,
      patient: {
        id: patientId,
        name: patient.name,
        email: patient.email
      },
      latestVital
    });

  } catch (err) {
    console.error("Erro em /patients/search:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ===========================
// EXPORTAR APP
// ===========================
exports.api = functions.https.onRequest(app);
