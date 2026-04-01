package com.hiketracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hiketracker.model.Hike;

import java.util.List;

@Dao
public interface HikeDao {

    @Insert
    long insertHike(Hike hike);

    @Query("SELECT * FROM hikes WHERE userId = :userId ORDER BY startTimeMillis DESC")
    LiveData<List<Hike>> getHikesForUser(String userId);

    @Query("SELECT * FROM hikes WHERE userId = :userId ORDER BY startTimeMillis DESC")
    List<Hike> getHikesForUserSync(String userId);

    @Query("DELETE FROM hikes WHERE id = :hikeId")
    void deleteHike(int hikeId);
}
