package com.dovar.dplugin.collector.res

class PluginResourceEntry {
    def type
    def name
    def id
    def _id
    List<PluginResourceSubEntry> entries

    public PluginResourceEntry(type, name, id, _id, entries) {
        this.type = type
        this.name = name
        this.id = id
        this._id = _id
        this.entries = entries
    }
}