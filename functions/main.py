from firebase_functions import https_fn
from firebase_functions.options import set_global_options
from firebase_admin import initialize_app, firestore
from flask import jsonify, Request

# Limite de instâncias
set_global_options(max_instances=10)

# Inicializa o Firebase Admin
initialize_app()
db = firestore.client()

# ===== GET /patients/<id> =====
@https_fn.on_request()
def get_patient(req: Request):
    # Apenas GET
    if req.method != "GET":
        return jsonify({"error": "Method not allowed"}), 405

    patient_id = req.path.strip("/").split("/")[-1]  # extrai id da URL
    doc = db.collection("users").document(patient_id).get()
    if doc.exists:
        return jsonify(doc.to_dict())
    return jsonify({"error": "Patient not found"}), 404

# ===== POST /patients/<id>/vitals =====
@https_fn.on_request()
def add_vital(req: Request):
    if req.method != "POST":
        return jsonify({"error": "Method not allowed"}), 405

    patient_id = req.path.strip("/").split("/")[-2]  # pega id da URL
    data = req.get_json()
    if not data:
        return jsonify({"error": "Missing body"}), 400

    data["patientId"] = patient_id
    doc_ref = db.collection("vitals").document()
    doc_ref.set(data)
    return jsonify({"id": doc_ref.id}), 201
