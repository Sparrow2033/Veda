package com.veda.app.demo;

public final class DemoDeleteResult {

    public final int demoNotesFound;
    public final int demoNotesDeleted;

    public DemoDeleteResult(int demoNotesFound, int demoNotesDeleted) {
        this.demoNotesFound = demoNotesFound;
        this.demoNotesDeleted = demoNotesDeleted;
    }
}