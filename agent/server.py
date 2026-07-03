from flask import Flask, request, jsonify, render_template
from flask_cors import CORS
import os

app = Flask(__name__, template_folder='.')
app.json.ensure_ascii = False
CORS(app, resources={r"/*": {"origins": "*"}})

from agent import MultiModelAgent, _build_direct_fol_problem, _run_proof_text

MODEL_TYPE = os.getenv('MODEL_TYPE', 'ollama').strip().lower()
agent = MultiModelAgent(MODEL_TYPE)


def _build_axioms_goal_query(axioms, goal):
    if not isinstance(axioms, list) or not axioms or not all(isinstance(item, str) and item.strip() for item in axioms):
        raise ValueError("Bitte uebergebe 'axioms' als nicht-leere Liste aus Formel-Strings.")
    if not isinstance(goal, str) or not goal.strip():
        raise ValueError("Bitte uebergebe 'goal' als nicht-leeren Formel-String.")
    return "Axiome:\n" + "\n".join(item.strip() for item in axioms) + "\n\nZiel:\n" + goal.strip()


def _parse_bool(value):
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"1", "true", "yes", "ja", "on"}
    return bool(value)


def _extract_machine_status(result_text):
    return result_text.splitlines()[0].strip().upper() if result_text else "FAILURE"


@app.route('/agent', methods=['POST'])
def agent_service():
    # Retrieve the JSON data from the request
    data = request.get_json()

    # Check if 'input_string' exists in the sent JSON
    if not data or 'message' not in data:
        return jsonify({"error": "Please provide a 'message' key in your JSON body"}), 400

    # extract the message
    message = data['message']

    # compute the result
    result = agent.agent(message)

    # Return the string back as a JSON response
    return jsonify({"result": result})


@app.route('/api/prove', methods=['POST'])
def api_prove_service():
    data = request.get_json(silent=True) or {}

    try:
        query = _build_axioms_goal_query(data.get('axioms'), data.get('goal'))
        direct_problem = _build_direct_fol_problem(query)
        if direct_problem is None:
            raise ValueError("Aus 'axioms' und 'goal' konnte kein pruefbares Problem gebaut werden.")
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400

    include_problem = _parse_bool(data.get('include_problem', False))
    result = _run_proof_text(direct_problem, verbose=False)
    status = _extract_machine_status(result)

    actions = data.get('actions')
    if actions is not None and not isinstance(actions, dict):
        return jsonify({"error": "'actions' muss ein JSON-Objekt sein."}), 400

    device_action = None
    if isinstance(actions, dict):
        action_key = 'on_success' if status == 'SUCCESS' else 'on_failure'
        device_action = actions.get(action_key)

    response = {
        "status": status,
        "device_action": device_action,
        "device_id": data.get('device_id'),
    }

    if include_problem:
        response["normalized_problem"] = direct_problem

    return jsonify(response)


@app.route('/', methods=['GET'])
def index():
    return render_template('index.html')


@app.route('/reset', methods=['POST'])
def reset_service():
    agent.reset()
    return ''


if __name__ == '__main__':
    # Run the server on localhost:5000
    app.run(debug=True, host='0.0.0.0', port=5000)


