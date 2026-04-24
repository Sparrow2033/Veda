package com.veda.app.demo;

public final class DemoImportResult {

    public final int subjectsCreated;
    public final int notesInserted;
    public final int notesUpdated;
    public final int linksResolved;

    public DemoImportResult(int subjectsCreated,
                            int notesInserted,
                            int notesUpdated,
                            int linksResolved) {
        this.subjectsCreated = subjectsCreated;
        this.notesInserted = notesInserted;
        this.notesUpdated = notesUpdated;
        this.linksResolved = linksResolved;
    }
}