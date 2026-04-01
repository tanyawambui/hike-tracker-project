package com.hiketracker.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.hiketracker.data.local.HikeDao;
import com.hiketracker.data.local.HikeDatabase;
import com.hiketracker.model.Hike;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HikeRepository {

    private final HikeDao hikeDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HikeRepository(Application application) {
        HikeDatabase db = HikeDatabase.getInstance(application);
        hikeDao = db.hikeDao();
    }

    public void insertHike(Hike hike, InsertCallback callback) {
        executor.execute(() -> {
            long id = hikeDao.insertHike(hike);
            if (callback != null) callback.onInserted(id);
        });
    }

    public LiveData<List<Hike>> getHikesForUser(String userId) {
        return hikeDao.getHikesForUser(userId);
    }

    public void deleteHike(int hikeId) {
        executor.execute(() -> hikeDao.deleteHike(hikeId));
    }

    public interface InsertCallback {
        void onInserted(long id);
    }
}
