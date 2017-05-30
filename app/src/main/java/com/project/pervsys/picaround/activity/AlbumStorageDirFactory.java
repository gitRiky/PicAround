package com.project.pervsys.picaround.activity;

import android.os.Environment;

import java.io.File;


class AlbumStorageDirFactory {

    public File getAlbumStorageDir(String albumName) {
        return new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ),
                albumName
        );
    }
}
