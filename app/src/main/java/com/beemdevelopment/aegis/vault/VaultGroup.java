package com.beemdevelopment.aegis.vault;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class VaultGroup extends UUIDMap.Value {
    private final String _name;

    public VaultGroup(String name) {
        this(UUID.randomUUID(), name);
    }

    private VaultGroup(UUID uuid, String name) {
        super(uuid);
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("uuid", getUUID().toString());
            obj.put("name", _name);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static VaultGroup fromJson(JSONObject obj) throws JSONException {
        UUID uuid = UUID.fromString(obj.getString("uuid"));
        return new VaultGroup(uuid, obj.getString("name"));
    }
}
