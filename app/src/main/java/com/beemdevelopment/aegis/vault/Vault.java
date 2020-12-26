package com.beemdevelopment.aegis.vault;

import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public class Vault {
    private static final int VERSION = 2;
    private final UUIDMap<VaultEntry> _entries = new UUIDMap<>();
    private final UUIDMap<VaultGroup> _groups = new UUIDMap<>();

    public JSONObject toJson() {
        try {
            JSONArray entries = new JSONArray();
            for (VaultEntry e : _entries) {
                entries.put(e.toJson());
            }

            JSONArray groups = new JSONArray();
            for (VaultGroup g : _groups) {
                groups.put(g.toJson());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", entries);
            obj.put("groups", groups);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Vault fromJson(JSONObject obj) throws VaultException {
        Vault vault = new Vault();
        UUIDMap<VaultEntry> entries = vault.getEntries();
        UUIDMap<VaultGroup> groups = vault.getGroups();

        try {
            int ver = obj.getInt("version");
            if (ver != VERSION) {
                throw new VaultException(String.format("Unsupported version: %d", ver));
            }

            JSONArray entriesJson = obj.getJSONArray("entries");
            for (int i = 0; i < entriesJson.length(); i++) {
                VaultEntry entry = VaultEntry.fromJson(entriesJson.getJSONObject(i));
                entries.add(entry);
            }

            JSONArray groupsJson = obj.getJSONArray("groups");
            for (int i = 0; i < groupsJson.length(); i++) {
                VaultGroup group = VaultGroup.fromJson(groupsJson.getJSONObject(i));
                groups.add(group);
            }

            // migrate old groups to new groups if necessary
            for (VaultEntry entry : entries) {
                Optional<VaultGroup> optGroup = groups.getValues().stream().filter(g -> g.getName().equals(entry.getOldGroup())).findFirst();
                if (entry.getOldGroup() != null) {
                    if (optGroup.isPresent()) {
                        entry.getGroups().add(optGroup.get().getUUID());
                    } else {
                        VaultGroup group = new VaultGroup(entry.getOldGroup());
                        groups.add(group);
                        entry.getGroups().add(group.getUUID());
                    }
                }
            }
        } catch (EncodingException | OtpInfoException | JSONException e) {
            throw new VaultException(e);
        }

        return vault;
    }

    public UUIDMap<VaultEntry> getEntries() {
        return _entries;
    }

    public UUIDMap<VaultGroup> getGroups() {
        return _groups;
    }
}
