/* SysLog - A simple logging tool
 * Copyright (C) 2020  Scott Warner <Tortel1210@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tortel.syslog;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import static android.provider.DocumentsContract.Root;
import static android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tortel.syslog.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Document provider so people can access the collected log zips in other
 * applications, such as email apps
 */
public class LogProvider extends DocumentsProvider {
    private static final String MIME_ZIP = "application/zip";

    private static final String[] DEFAULT_ROOT_PROJECTION =
            new String[]{
                    Root.COLUMN_ROOT_ID,
                    Root.COLUMN_ICON,
                    Root.COLUMN_TITLE,
                    Root.COLUMN_FLAGS,
                    Root.COLUMN_DOCUMENT_ID
            };
    private static final String[] DEFAULT_DOC_PROJECTION =
            new String[]{
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_MIME_TYPE,
                    Document.COLUMN_FLAGS,
                    Document.COLUMN_SIZE,
                    Document.COLUMN_LAST_MODIFIED
            };

    private static final int ROOT_ID = 1;
    private static final int ROOT_DOCUMENT_ID = 2;
    private static final int FIRST_DOCUMENT_ID = 10;

    /**
     * Get the sorted list of zips
     * @return the file of zip files, or null if there is an error/none
     */
    private File[] getZipList() {
        File[] files = null;
        File rootZipDir = FileUtils.getZipDir(getContext());
        if (rootZipDir.exists() && rootZipDir.isDirectory()) {
            files = rootZipDir.listFiles();
            if (files != null) {
                // Sort it
                Arrays.sort(files, (a, b) ->
                        a.getName().compareTo(b.getName()));
            }
        }
        return files;
    }

    /**
     * Find the zip file by document ID
     * @param documentId the document id
     * @return a reference to the file
     * @throws FileNotFoundException if the file is not found
     */
    private File findZipByDocumentId(@NonNull String documentId) throws FileNotFoundException {
        return findZipByDocumentId(Integer.parseInt(documentId));
    }

    /**
     * Find the zip file by document ID
     * @param documentId the document id
     * @return a reference to the file
     * @throws FileNotFoundException if the file is not found
     */
    private File findZipByDocumentId(int documentId) throws FileNotFoundException {
        int index = documentId - FIRST_DOCUMENT_ID;
        if (index < 0) {
            throw new FileNotFoundException();
        }
        File[] files = getZipList();
        if (index > files.length) {
            throw new FileNotFoundException();
        }
        return files[index];
    }

    /**
     * Add a row to the cursor representing the file
     * @param result the cursor
     * @param file the file
     * @param id the document id
     */
    private void addRowForFile(@NonNull MatrixCursor result, @NonNull File file, int id) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, id);
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(Document.COLUMN_MIME_TYPE, file.isFile() ? MIME_ZIP: Document.MIME_TYPE_DIR);
        // No flags for files ?
        row.add(Document.COLUMN_FLAGS, file.isFile() ? 0 : Document.FLAG_DIR_PREFERS_LAST_MODIFIED);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, ROOT_ID);
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.app_name));
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY);
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID);
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        int originalId = Integer.parseInt(documentId);
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOC_PROJECTION);
        File file;
        if (originalId == ROOT_DOCUMENT_ID) {
            file = FileUtils.getZipDir(getContext());
        } else {
            file = findZipByDocumentId(originalId);
        }
        addRowForFile(result, file, originalId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOC_PROJECTION);
        if (parentDocumentId.equals("" + ROOT_DOCUMENT_ID)) {
            File[] files = getZipList();
            if (files != null) {
                // Sort it
                Arrays.sort(files, (a, b) ->
                        a.getName().compareTo(b.getName()));

                // Generate IDs based on the index past the first document ID
                for (int i = 0; i < files.length; i++) {
                    addRowForFile(result, files[i], FIRST_DOCUMENT_ID + i);
                }
            }
        } else {
            throw new FileNotFoundException();
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        File file = findZipByDocumentId(documentId);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public boolean onCreate() {
        return true;
    }
}
