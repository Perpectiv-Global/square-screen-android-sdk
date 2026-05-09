package io.squarescreen.cache.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.squarescreen.cache.db.entity.PlaylistEntity;
import io.squarescreen.cache.db.entity.PlaylistItemEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlaylistDao_Impl implements PlaylistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlaylistEntity> __insertionAdapterOfPlaylistEntity;

  private final EntityInsertionAdapter<PlaylistItemEntity> __insertionAdapterOfPlaylistItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllItems;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlaylist;

  public PlaylistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaylistEntity = new EntityInsertionAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlist` (`id`,`cachedAt`,`strategyLoop`,`strategyShuffle`,`strategyPreloadCount`,`scheduleUuid`,`scheduleName`,`schedulePriority`,`playlistUuid`,`playlistName`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCachedAt());
        final Integer _tmp = entity.getStrategyLoop() == null ? null : (entity.getStrategyLoop() ? 1 : 0);
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, _tmp);
        }
        final Integer _tmp_1 = entity.getStrategyShuffle() == null ? null : (entity.getStrategyShuffle() ? 1 : 0);
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        if (entity.getStrategyPreloadCount() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getStrategyPreloadCount());
        }
        if (entity.getScheduleUuid() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getScheduleUuid());
        }
        if (entity.getScheduleName() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getScheduleName());
        }
        if (entity.getSchedulePriority() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getSchedulePriority());
        }
        if (entity.getPlaylistUuid() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPlaylistUuid());
        }
        if (entity.getPlaylistName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPlaylistName());
        }
      }
    };
    this.__insertionAdapterOfPlaylistItemEntity = new EntityInsertionAdapter<PlaylistItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlist_item` (`id`,`playlistId`,`type`,`url`,`duration`,`width`,`height`,`transition`,`title`,`thumbnail`,`sortOrder`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getPlaylistId());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getUrl());
        statement.bindLong(5, entity.getDuration());
        if (entity.getWidth() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getWidth());
        }
        if (entity.getHeight() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getHeight());
        }
        if (entity.getTransition() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getTransition());
        }
        if (entity.getTitle() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTitle());
        }
        if (entity.getThumbnail() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getThumbnail());
        }
        statement.bindLong(11, entity.getSortOrder());
      }
    };
    this.__preparedStmtOfDeleteAllItems = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlist_item WHERE playlistId = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlist";
        return _query;
      }
    };
  }

  @Override
  public Object upsertPlaylist(final PlaylistEntity playlist,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistEntity.insert(playlist);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertItems(final List<PlaylistItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replacePlaylist(final PlaylistEntity playlist, final List<PlaylistItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> PlaylistDao.DefaultImpls.replacePlaylist(PlaylistDao_Impl.this, playlist, items, __cont), $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> PlaylistDao.DefaultImpls.clearAll(PlaylistDao_Impl.this, __cont), $completion);
  }

  @Override
  public Object deleteAllItems(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllItems.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllItems.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaylist(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlaylist.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePlaylist.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPlaylist(final Continuation<? super PlaylistEntity> $completion) {
    final String _sql = "SELECT * FROM playlist WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlaylistEntity>() {
      @Override
      @Nullable
      public PlaylistEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final int _cursorIndexOfStrategyLoop = CursorUtil.getColumnIndexOrThrow(_cursor, "strategyLoop");
          final int _cursorIndexOfStrategyShuffle = CursorUtil.getColumnIndexOrThrow(_cursor, "strategyShuffle");
          final int _cursorIndexOfStrategyPreloadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "strategyPreloadCount");
          final int _cursorIndexOfScheduleUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleUuid");
          final int _cursorIndexOfScheduleName = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleName");
          final int _cursorIndexOfSchedulePriority = CursorUtil.getColumnIndexOrThrow(_cursor, "schedulePriority");
          final int _cursorIndexOfPlaylistUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistUuid");
          final int _cursorIndexOfPlaylistName = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistName");
          final PlaylistEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            final Boolean _tmpStrategyLoop;
            final Integer _tmp;
            if (_cursor.isNull(_cursorIndexOfStrategyLoop)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfStrategyLoop);
            }
            _tmpStrategyLoop = _tmp == null ? null : _tmp != 0;
            final Boolean _tmpStrategyShuffle;
            final Integer _tmp_1;
            if (_cursor.isNull(_cursorIndexOfStrategyShuffle)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getInt(_cursorIndexOfStrategyShuffle);
            }
            _tmpStrategyShuffle = _tmp_1 == null ? null : _tmp_1 != 0;
            final Integer _tmpStrategyPreloadCount;
            if (_cursor.isNull(_cursorIndexOfStrategyPreloadCount)) {
              _tmpStrategyPreloadCount = null;
            } else {
              _tmpStrategyPreloadCount = _cursor.getInt(_cursorIndexOfStrategyPreloadCount);
            }
            final String _tmpScheduleUuid;
            if (_cursor.isNull(_cursorIndexOfScheduleUuid)) {
              _tmpScheduleUuid = null;
            } else {
              _tmpScheduleUuid = _cursor.getString(_cursorIndexOfScheduleUuid);
            }
            final String _tmpScheduleName;
            if (_cursor.isNull(_cursorIndexOfScheduleName)) {
              _tmpScheduleName = null;
            } else {
              _tmpScheduleName = _cursor.getString(_cursorIndexOfScheduleName);
            }
            final Integer _tmpSchedulePriority;
            if (_cursor.isNull(_cursorIndexOfSchedulePriority)) {
              _tmpSchedulePriority = null;
            } else {
              _tmpSchedulePriority = _cursor.getInt(_cursorIndexOfSchedulePriority);
            }
            final String _tmpPlaylistUuid;
            if (_cursor.isNull(_cursorIndexOfPlaylistUuid)) {
              _tmpPlaylistUuid = null;
            } else {
              _tmpPlaylistUuid = _cursor.getString(_cursorIndexOfPlaylistUuid);
            }
            final String _tmpPlaylistName;
            if (_cursor.isNull(_cursorIndexOfPlaylistName)) {
              _tmpPlaylistName = null;
            } else {
              _tmpPlaylistName = _cursor.getString(_cursorIndexOfPlaylistName);
            }
            _result = new PlaylistEntity(_tmpId,_tmpCachedAt,_tmpStrategyLoop,_tmpStrategyShuffle,_tmpStrategyPreloadCount,_tmpScheduleUuid,_tmpScheduleName,_tmpSchedulePriority,_tmpPlaylistUuid,_tmpPlaylistName);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPlaylistItems(final Continuation<? super List<PlaylistItemEntity>> $completion) {
    final String _sql = "SELECT * FROM playlist_item WHERE playlistId = 1 ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PlaylistItemEntity>>() {
      @Override
      @NonNull
      public List<PlaylistItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfTransition = CursorUtil.getColumnIndexOrThrow(_cursor, "transition");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<PlaylistItemEntity> _result = new ArrayList<PlaylistItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getInt(_cursorIndexOfPlaylistId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final Integer _tmpWidth;
            if (_cursor.isNull(_cursorIndexOfWidth)) {
              _tmpWidth = null;
            } else {
              _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            }
            final Integer _tmpHeight;
            if (_cursor.isNull(_cursorIndexOfHeight)) {
              _tmpHeight = null;
            } else {
              _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            }
            final String _tmpTransition;
            if (_cursor.isNull(_cursorIndexOfTransition)) {
              _tmpTransition = null;
            } else {
              _tmpTransition = _cursor.getString(_cursorIndexOfTransition);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpThumbnail;
            if (_cursor.isNull(_cursorIndexOfThumbnail)) {
              _tmpThumbnail = null;
            } else {
              _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new PlaylistItemEntity(_tmpId,_tmpPlaylistId,_tmpType,_tmpUrl,_tmpDuration,_tmpWidth,_tmpHeight,_tmpTransition,_tmpTitle,_tmpThumbnail,_tmpSortOrder);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
