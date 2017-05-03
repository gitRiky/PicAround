package com.project.pervsys.picaround;

import android.os.Environment;

import java.io.File;

import static android.os.Environment.getExternalStorageDirectory;


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
