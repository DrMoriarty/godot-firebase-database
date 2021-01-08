extends Node

signal get_value(path, value)
signal set_value(path, value)
signal remove_value(path, value)
signal inited
var _db = null
var _web = false
var _db_root = []
var inited = false

func _ready():
    if type_exists('FirebaseDatabase'):
        _db = ClassDB.instance('FirebaseDatabase')
    elif Engine.has_singleton('FirebaseDatabase'):
        _db = Engine.get_singleton('FirebaseDatabase')
    elif OS.has_feature('HTML5'):
        _web = true
        set_process(true)
        JavaScript.eval("""
        var _db_get_value_cache = null;
        var _db_set_value_cache = null;
        function _db_get_value_result(key, result) {
            if(_db_get_value_cache === null) _db_get_value_cache = {};
            _db_get_value_cache[key] = result;
        }
        function _db_set_value_result(key, result) {
            if(_db_set_value_cache === null) _db_set_value_cache = {};
            _db_set_value_cache[key] = result;
        }
        function _get_db_get_value() {
            if(_db_get_value_cache === null) return null;
            var res = JSON.stringify(_db_get_value_cache);
            _db_get_value_cache = null;
            return res;
        }
        function _get_db_set_value() {
            if(_db_set_value_cache === null) return null;
            var res = JSON.stringify(_db_set_value_cache);
            _db_set_value_cache = null;
            return res;
        }
        """, true)
    else:
        push_warning('FirebaseDatabase module not found!')
    if _db != null:
        _db.connect('get_value', self, '_on_get_value')
        _db.connect('child_added', self, '_on_child_added')
        _db.connect('child_changed', self, '_on_child_changed')
        _db.connect('child_moved', self, '_on_child_moved')
        _db.connect('child_removed', self, '_on_child_removed')
    auth.connect('logged_in', self, '_on_logged_in')
    auth.connect('logged_out', self, '_on_logged_out')
    if auth.is_logged_in():
        _on_logged_in();

func _process(dt):
    # get web results
    var get_cache = JavaScript.eval("_get_db_get_value();", false)
    if get_cache != null:
        var res = JSON.parse(get_cache)
        if res.error == OK:
            var cache = res.result
            for key in cache:
                emit_signal('get_value', key, cache[key])
    var set_cache = JavaScript.eval("_get_db_set_value();", false)
    if set_cache != null:
        var res = JSON.parse(set_cache)
        if res.error == OK:
            var cache = res.result
            for key in cache:
                emit_signal('set_value', key, cache[key])

func _on_get_value(path, value):
    emit_signal('get_value', path, value)

func _on_child_added(path, value):
    emit_signal('set_value', path, value)

func _on_child_changed(path, value):
    emit_signal('set_value', path, value)

func _on_child_moved(path, value):
    emit_signal('set_value', path, value)

func _on_child_removed(path, value):
    emit_signal('remove_value', path, value)

func _on_logged_in():
    # set default data base path
    if _db != null:
        print('Database inited')
        _db.set_db_root(['users', auth.uid()])
        inited = true
        emit_signal('inited')
    elif _web:
        print('Database inited')
        _set_db_root(['users', auth.uid()])
        inited = true
        emit_signal('inited')
        # load full db content
        get_value()

func _on_logged_out():
    if _db != null:
        inited = false
    elif _web:
        inited = false

func _set_db_root(keys):
    if _web:
        _db_root = keys
        #var path = _db_root()
        #var key = keys[keys.size()-1]
        #JavaScript.eval("""
        #firebase.database().ref('%s').on('value', function(snapshot) {
        #    var result = snapshot.val();
        #    console.log('DB set value result:', result);
        #    _db_set_value_result('%s', result);
        #});
        #"""%[path, key], false)

func _db_root(keys = []):
    if _web:
        var path = '/'
        for el in _db_root:
            if path.length() > 1:
                path += '/'
            path += el
        for el in keys:
            if path.length() > 1:
                path += '/'
            path += el
        return path
    else:
        return null

# Public methods

func set_value(keys, value):
    if typeof(keys) != TYPE_ARRAY:
        keys = [keys]
    if _db != null and inited:
        _db.set_value(keys, value)
    elif _web and inited:
        var path = _db_root(keys)
        JavaScript.eval("""
        firebase.database().ref('%s').set(%s);
        """%[path, JSON.print(value)], false)

func push_child(keys):
    if typeof(keys) != TYPE_ARRAY:
        keys = [keys]
    if _db != null and inited:
        _db.push_child(keys)
    elif _web and inited:
        var path = _db_root(keys)
        var result = JavaScript.eval("""
        firebase.database().ref('%s').push().key;
        """%[path], false)

func update_children(paths, params):
    if _db != null and inited:
        _db.update_children(paths, params)
    elif _web and inited:
        var updates = {}
        for key in paths:
            var path = _db_root([key])
            updates[path] = params
        JavaScript.eval("""
        firebase.database().ref().update(%s);
        """%[JSON.print(updates)], false)

func remove_value(keys):
    if typeof(keys) != TYPE_ARRAY:
        keys = [keys]
    if _db != null and inited:
        _db.remove_value(keys)
    elif _web and inited:
        var path = _db_root(keys)
        JavaScript.eval("""
        firebase.database().ref('%s').remove();
        """%[path], false)

func get_value(keys = null):
    if keys == null:
        keys = []
    elif typeof(keys) != TYPE_ARRAY:
        keys = [keys]
    if _db != null and inited:
        _db.get_value(keys)
    elif _web and inited:
        var path = _db_root(keys)
        var key = path.get_file()  #keys[keys.size()-1]
        JavaScript.eval("""
        firebase.database().ref('%s').once('value').then(function(snapshot) {
            var result = snapshot.val();
            console.log('DB get value result:', result);
            _db_get_value_result('%s', result);
        });
        """%[path, key], false)

