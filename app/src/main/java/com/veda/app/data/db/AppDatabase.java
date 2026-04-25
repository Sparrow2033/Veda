package com.veda.app.data.db;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.veda.app.data.dao.HomeworkDao;
import com.veda.app.data.dao.LinkDao;
import com.veda.app.data.dao.NoteDao;
import com.veda.app.data.dao.SubjectDao;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.entity.LinkEntity;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.SubjectEntity;

import java.util.HashSet;
import java.util.Set;

@Database(
        entities = {
                SubjectEntity.class,
                NoteEntity.class,
                LinkEntity.class,
                HomeworkEntity.class
        },
        version = 7,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "veda.db";
    private static volatile AppDatabase INSTANCE;

    public abstract SubjectDao subjectDao();
    public abstract NoteDao noteDao();
    public abstract LinkDao linkDao();
    public abstract HomeworkDao homeworkDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "notes", "tags")) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''");
            }
        }
    };

    public static final Migration MIGRATION_2_5 = new Migration(2, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            createHomeworkV6IfMissing(db);
            cleanupUnknownTables(db);
        }
    };

    public static final Migration MIGRATION_3_5 = new Migration(3, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            createHomeworkV6IfMissing(db);
            cleanupUnknownTables(db);
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            createHomeworkV6IfMissing(db);
            cleanupUnknownTables(db);
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            cleanupUnknownTables(db);

            if (!tableExists(db, "homework")) {
                createHomeworkV6IfMissing(db);
                return;
            }

            if (columnExists(db, "homework", "noteId")) {
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `homework_new` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`subjectId` INTEGER NOT NULL, " +
                                "`title` TEXT NOT NULL, " +
                                "`description` TEXT NOT NULL, " +
                                "`dueDate` INTEGER NOT NULL, " +
                                "`status` INTEGER NOT NULL, " +
                                "`priority` INTEGER NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL, " +
                                "`updatedAt` INTEGER NOT NULL, " +
                                "FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                                ")"
                );

                db.execSQL(
                        "INSERT INTO `homework_new` (`id`,`subjectId`,`title`,`description`,`dueDate`,`status`,`priority`,`createdAt`,`updatedAt`) " +
                                "SELECT `id`,`subjectId`,`title`,`description`,`dueDate`,`status`,`priority`,`createdAt`,`updatedAt` FROM `homework`"
                );

                db.execSQL("DROP TABLE IF EXISTS `homework`");
                db.execSQL("ALTER TABLE `homework_new` RENAME TO `homework`");

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_subjectId` ON `homework` (`subjectId`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_dueDate` ON `homework` (`dueDate`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_status` ON `homework` (`status`)");
            } else {
                createHomeworkV6IfMissing(db);
            }
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_dueDate_updatedAt` ON `homework` (`dueDate`, `updatedAt`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_status_dueDate_updatedAt` ON `homework` (`status`, `dueDate`, `updatedAt`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_subjectId_updatedAt` ON `notes` (`subjectId`, `updatedAt`)");

            db.execSQL(
                    "DELETE FROM `links` " +
                            "WHERE `id` NOT IN (" +
                            "SELECT MIN(`id`) FROM `links` GROUP BY `fromNoteId`, `toNoteId`" +
                            ")"
            );
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_links_fromNoteId_toNoteId` ON `links` (`fromNoteId`, `toNoteId`)");
        }
    };

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_5,
                                    MIGRATION_3_5,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6,
                                    MIGRATION_6_7
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void createHomeworkV6IfMissing(@NonNull SupportSQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS `homework` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`dueDate` INTEGER NOT NULL, " +
                        "`status` INTEGER NOT NULL, " +
                        "`priority` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_subjectId` ON `homework` (`subjectId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_dueDate` ON `homework` (`dueDate`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_status` ON `homework` (`status`)");
    }

    private static void cleanupUnknownTables(@NonNull SupportSQLiteDatabase db) {
        Set<String> keep = new HashSet<>();
        keep.add("subjects");
        keep.add("notes");
        keep.add("links");
        keep.add("homework");
        keep.add("android_metadata");
        keep.add("sqlite_sequence");
        keep.add("room_master_table");

        Cursor c = db.query("SELECT name FROM sqlite_master WHERE type='table'");
        try {
            int idx = c.getColumnIndex("name");
            if (idx == -1) return;
            while (c.moveToNext()) {
                String name = c.getString(idx);
                if (name == null) continue;

                if (keep.contains(name)) continue;
                if (name.startsWith("sqlite_")) continue;
                if (name.startsWith("room_")) continue;

                db.execSQL("DROP TABLE IF EXISTS `" + name + "`");
            }
        } finally {
            c.close();
        }
    }

    private static boolean tableExists(@NonNull SupportSQLiteDatabase db, @NonNull String tableName) {
        Cursor c = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private static boolean columnExists(@NonNull SupportSQLiteDatabase db,
                                        @NonNull String tableName,
                                        @NonNull String columnName) {
        Cursor cursor = db.query("PRAGMA table_info(`" + tableName + "`)");
        try {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex == -1) return false;
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                if (columnName.equals(name)) return true;
            }
            return false;
        } finally {
            cursor.close();
        }
    }
}
