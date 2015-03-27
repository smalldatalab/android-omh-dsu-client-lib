package io.smalldatalab.omhclient;

import com.orm.SugarRecord;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by changun on 3/26/15.
 */
public class DSUDataPoint extends SugarRecord<DSUDataPoint> {

    String schemaNamespace;
    String schemaName;
    String schemaVersion;
    String acquisitionSource;
    String acquisitionModality;
    String creationDateTime;
    String body;


    protected DSUDataPoint(String schemaNamespace, String schemaName, String schemaVersion, String acquisitionSource, String acquisitionModality, String creationDateTime, String body) {
        this.schemaNamespace = schemaNamespace;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
        this.acquisitionSource = acquisitionSource;
        this.acquisitionModality = acquisitionModality;
        this.creationDateTime = creationDateTime;
        this.body = body;
    }

    public DSUDataPoint() {
        // for Sugar ORM
    }


    @Override
    public String toString() {
        return "DSUDataPoint{" +
                "schemaNamespace='" + schemaNamespace + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", schemaVersion='" + schemaVersion + '\'' +
                ", acquisitionSource='" + acquisitionSource + '\'' +
                ", acquisitionModality='" + acquisitionModality + '\'' +
                ", creationDateTime=" + creationDateTime +
                ", body='" + body + '\'' +
                '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject header = new JSONObject();
        header.put("id", String.valueOf(UUID.randomUUID()));
        header.put("creation_date_time", creationDateTime);

        JSONObject schemaId = new JSONObject();
        schemaId.put("name", schemaName);
        schemaId.put("namespace", schemaNamespace);
        schemaId.put("version", schemaVersion);

        if (acquisitionSource != null) {
            JSONObject acquisition = new JSONObject();
            acquisition.put("source_name", acquisitionSource);
            acquisition.put("modality", acquisitionModality);
            header.put("acquisition_provenance", acquisition);
        }

        header.put("schema_id", schemaId);


        JSONObject body = new JSONObject(this.body);

        JSONObject datapoint = new JSONObject();
        datapoint.put("header", header);
        datapoint.put("body", body);
        return datapoint;
    }

}
