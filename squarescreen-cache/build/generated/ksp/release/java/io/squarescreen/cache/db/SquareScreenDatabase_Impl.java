package io.squarescreen.cache.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import io.squarescreen.cache.db.dao.PlaylistDao;
import io.squarescreen.cache.db.dao.PlaylistDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SquareScreenDatabase_Impl extends SquareScreenDatabase {
  private volatile PlaylistDao _playlistDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist` (`id` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, `strategyLoop` INTEGER, `strategyShuffle` INTEGER, `strategyPreloadCount` INTEGER, `scheduleUuid` TEXT, `scheduleName` TEXT, `schedulePriority` INTEGER, `playlistUuid` TEXT, `playlistName` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_item` (`id` TEXT NOT NULL, `playlistId` INTEGER NOT NULL, `type` TEXT NOT NULL, `url` TEXT NOT NULL, `duration` INTEGER NOT NULL, `width` INTEGER, `height` INTEGER, `transition` TEXT, `title` TEXT, `thumbnail` TEXT, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_item_playlistId` ON `playlist_item` (`playlistId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9de9365cd6676623c3d858f031aa4676')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `playlist`");
        db.execSQL("DROP TABLE IF EXISTS `playlist_item`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPlaylist = new HashMap<String, TableInfo.Column>(10);
        _columnsPlaylist.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("strategyLoop", new TableInfo.Column("strategyLoop", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("strategyShuffle", new TableInfo.Column("strategyShuffle", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("strategyPreloadCount", new TableInfo.Column("strategyPreloadCount", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("scheduleUuid", new TableInfo.Column("scheduleUuid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("scheduleName", new TableInfo.Column("scheduleName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("schedulePriority", new TableInfo.Column("schedulePriority", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("playlistUuid", new TableInfo.Column("playlistUuid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylist.put("playlistName", new TableInfo.Column("playlistName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylist = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylist = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylist = new TableInfo("playlist", _columnsPlaylist, _foreignKeysPlaylist, _indicesPlaylist);
        final TableInfo _existingPlaylist = TableInfo.read(db, "playlist");
        if (!_infoPlaylist.equals(_existingPlaylist)) {
          return new RoomOpenHelper.ValidationResult(false, "playlist(io.squarescreen.cache.db.entity.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylist + "\n"
                  + " Found:\n" + _existingPlaylist);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylistItem = new HashMap<String, TableInfo.Column>(11);
        _columnsPlaylistItem.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("width", new TableInfo.Column("width", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("height", new TableInfo.Column("height", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("transition", new TableInfo.Column("transition", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("thumbnail", new TableInfo.Column("thumbnail", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistItem.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylistItem = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPlaylistItem.add(new TableInfo.ForeignKey("playlist", "CASCADE", "NO ACTION", Arrays.asList("playlistId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPlaylistItem = new HashSet<TableInfo.Index>(1);
        _indicesPlaylistItem.add(new TableInfo.Index("index_playlist_item_playlistId", false, Arrays.asList("playlistId"), Arrays.asList("ASC")));
        final TableInfo _infoPlaylistItem = new TableInfo("playlist_item", _columnsPlaylistItem, _foreignKeysPlaylistItem, _indicesPlaylistItem);
        final TableInfo _existingPlaylistItem = TableInfo.read(db, "playlist_item");
        if (!_infoPlaylistItem.equals(_existingPlaylistItem)) {
          return new RoomOpenHelper.ValidationResult(false, "playlist_item(io.squarescreen.cache.db.entity.PlaylistItemEntity).\n"
                  + " Expected:\n" + _infoPlaylistItem + "\n"
                  + " Found:\n" + _existingPlaylistItem);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9de9365cd6676623c3d858f031aa4676", "890daac5259b5f52476661666437dedd");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "playlist","playlist_item");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `playlist`");
      _db.execSQL("DELETE FROM `playlist_item`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }
}
