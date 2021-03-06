import datetime
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
from flask import make_response, current_app, request, Blueprint
import os

api_v1 = Blueprint('api_v1', __name__)

cred = credentials.Certificate('/app/firebase-admin-sdk-key.json')
# Initialize the app with a custom auth variable, limiting the server's access
firebase_admin.initialize_app(cred, {
    'databaseURL': os.environ.get('DB_URL'),
})


@api_v1.route('/alive', methods=['GET'])
def alive():
    return {'status': 'OK'}, 200


@api_v1.route('/tasks', methods=['POST'])
def tasks():
    data = request.get_json()
    tasks_ref = db.reference('/tasks')
    new_task_ref = tasks_ref.push()
    new_task_ref.set(data)
    new_task_id = new_task_ref.key
    project_id = data.get('projectId')
    project_tasks_ref = db.reference('/projects/').child(project_id).child('tasks')
    project_tasks_ref.update({new_task_id: True})
    log_ref = db.reference('/log')
    log_event_ref = log_ref.child(project_id).child(new_task_id).push()
    log_event_ref.set({'eventType': 'CREATED', 'description': 'Task Created', 'timestamp': datetime.datetime.utcnow().replace(microsecond=0).isoformat() + 'Z'})
    return {'id': new_task_id }, 201


@api_v1.route('/tasks/<task_id>', methods=['PUT'])
def task_with_id(task_id):
    data = request.get_json()
    task_ref = db.reference('/tasks').child(task_id)
    task_ref.update(data)
    log_ref = db.reference('/log')
    project_id = task_ref.child('projectId').get()
    log_event_ref = log_ref.child(project_id).child(task_id).push()
    log_event_ref.set({'eventType': 'STATUS', 'description': 'Task status changed to ' + data['status'],
                       'timestamp': datetime.datetime.utcnow().replace(microsecond=0).isoformat() + 'Z'})

    return jsonify_no_content()


@api_v1.route('/tasks/<task_id>/users', methods=['PUT'])
def task_users(task_id):
    user_ids = request.get_json().get('userIds', [])
    array_as_dict = array_to_fb_object(user_ids)
    task_ref = db.reference('/tasks').child(task_id)
    task_users_ref = task_ref.child('users')
    task_users_ref.update(array_as_dict)
    log_ref = db.reference('/log')
    project_id = task_ref.child('projectId').get()
    for user_id in user_ids:
        user_ref = db.reference('/users').child(user_id)
        user_tasks_ref = user_ref.child('tasks')
        user_tasks_ref.update({task_id: True})
        log_event_ref = log_ref.child(project_id).child(task_id).push()
        log_event_ref.set({'eventType': 'ASSIGNMENT', 'description': 'User ' + user_ref.child('username').get() + ' assigned to task',
                           'timestamp': datetime.datetime.utcnow().replace(microsecond=0).isoformat() + 'Z'})
        return jsonify_no_content()


@api_v1.route('/projects', methods=['POST'])
def projects():
    projects_ref = db.reference('/projects')
    data = request.get_json()
    keywords = data.get('keywords')
    if keywords is not None:
        data['keywords'] = array_to_fb_object(keywords)
    date = datetime.datetime.utcnow().replace(microsecond=0).isoformat() + 'Z'
    data['creationDate'] = date
    data['lastModifiedDate'] = date
    new_project_ref = projects_ref.push()
    new_project_ref.set(data)
    return {'id': new_project_ref.key }, 201


@api_v1.route('/projects/<project_id>', methods=['DELETE'])
def project_with_id(project_id):

    project_ref = db.reference('/projects').child(project_id)
    tasks_value = project_ref.child('tasks').get()
    task_ids = []
    if tasks_value is not None:
        task_ids = tasks_value.keys()

    users_in_task_value = project_ref.child('members').get()
    users_in_project = []
    if users_in_task_value is not None:
        users_in_project = users_in_task_value.keys()

    for task_id in task_ids:
        task_ref = db.reference('/tasks').child(task_id)
        users_with_task_value = task_ref.child('users').get()
        users_with_task = []
        if users_with_task_value is not None:
            users_with_task = users_with_task_value.keys()
        for user_id in users_with_task:
            user_task_ref = db.reference('/users').child(user_id).child('tasks').child(task_id)
            delete_resource(user_task_ref)
        delete_resource(task_ref)

    for user_id in users_in_project:
        user_project_ref = db.reference('/users').child(user_id).child('projects').child(project_id)
        delete_resource(user_project_ref)

    delete_resource(project_ref)
    return jsonify_no_content()


@api_v1.route('/projects/<project_id>/members', methods=['PUT'])
def project_members(project_id):
    user_ids = request.get_json().get('userIds', [])
    array_as_dict = array_to_fb_object(user_ids)
    project_members_ref = db.reference('/projects').child(project_id).child('members')
    project_members_ref.update(array_as_dict)
    for user_id in user_ids:
        user_tasks_ref = db.reference('/users').child(user_id).child('projects')
        user_tasks_ref.update({project_id: True})
    return jsonify_no_content()


def jsonify_no_content():
    response = make_response('', 204)
    response.mimetype = current_app.config['JSONIFY_MIMETYPE']

    return response


def delete_resource(db_ref):
    db_ref.set({})


def array_to_fb_object(array):
    return dict.fromkeys(array, True)
