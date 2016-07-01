package io.smalldatalab.omhclient;

import org.json.JSONObject;

import java.util.Calendar;

public class DSUDataPointBuilder {
    private String schemaNamespace;
    private String schemaName;
    private String schemaVersion;
    private String acquisitionSource = null;
    private String acquisitionModality = null;
    private Calendar creationDateTime;
    private String body;

    public DSUDataPointBuilder setSchemaNamespace(String schemaNamespace) {
        this.schemaNamespace = schemaNamespace;
        return this;
    }

    public DSUDataPointBuilder setSchemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public DSUDataPointBuilder setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    public DSUDataPointBuilder setAcquisitionSource(String acquisitionSource) {
        this.acquisitionSource = acquisitionSource;
        return this;
    }

    public DSUDataPointBuilder setAcquisitionModality(String acquisitionModality) {
        this.acquisitionModality = acquisitionModality;
        return this;
    }

    public DSUDataPointBuilder setCreationDateTime(Calendar creationDateTime) {
        this.creationDateTime = creationDateTime;
        return this;
    }

    public DSUDataPointBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    public DSUDataPointBuilder setBody(JSONObject body) {
        return this.setBody(body.toString());
    }

    public DSUDataPoint createDSUDataPoint() {
        String creationDateTimeString = creationDateTime == null ? ISO8601.now() : ISO8601.fromCalendar(creationDateTime);
        return new DSUDataPoint(
                schemaNamespace,
                schemaName,
                schemaVersion,
                acquisitionSource,
                acquisitionModality,
                creationDateTimeString,
                body);
    }
}