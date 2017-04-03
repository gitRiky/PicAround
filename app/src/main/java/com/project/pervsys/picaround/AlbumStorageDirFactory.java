package com.project.pervsys.picaround;

import android.os.Environment;

import java.io.File;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by federico on 27/03/17.
 */

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
