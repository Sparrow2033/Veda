
package com.veda.app.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "links",
        indices = {
                @Index("fromNoteId"),
                @Index("toNoteId"),
                @Index(value = {"fromNoteId", "toNoteId"}, unique = true)
        },
        foreignKeys = {
                @ForeignKey(
                        entity = NoteEntity.class,
                        parentColumns = "id",
                        childColumns = "fromNoteId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = NoteEntity.class,
                        parentColumns = "id",
                        childColumns = "toNoteId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class LinkEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long fromNoteId;
    public long toNoteId;
    public long createdAt;

    public LinkEntity(long fromNoteId, long toNoteId, long createdAt) {
        this.fromNoteId = fromNoteId;
        this.toNoteId = toNoteId;
        this.createdAt = createdAt;
    }
}
