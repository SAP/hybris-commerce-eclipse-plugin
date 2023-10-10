/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.ytypesystem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BundlePackager {
	
	public static ByteArrayOutputStream buildZip(List<File> filesToZip) throws IOException {
        
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zOutStream = new ZipOutputStream(baos);
        zOutStream.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (File fileToZip : filesToZip) {
            if (fileToZip.isDirectory()) {
                dirZip(zOutStream, "", fileToZip);
            }
            else {
                fileZip(zOutStream, "", fileToZip);
            }
        }
        zOutStream.flush();
        zOutStream.close();
        
        return baos;
    }

    private static void dirZip(ZipOutputStream zOutputStream, String filePath, File fileDir) throws IOException {
        if (!fileDir.canRead()) {
            return;
        }

        File[] filesToZip = fileDir.listFiles();
        filePath = computeFilePath(filePath, fileDir.getName());

        for (File fileToZip : filesToZip) {
            if (fileToZip.isDirectory()) {
                dirZip(zOutputStream, filePath, fileToZip);
            }
            else {
                fileZip(zOutputStream, filePath, fileToZip);
            }
        }
    }

    private static void fileZip(ZipOutputStream zOutputStream, String filePath, File fileDir) throws IOException {
        if (!fileDir.canRead()) {
            return;
        }

		zOutputStream.putNextEntry(new ZipEntry(computeFilePath(filePath, fileDir.getName())));
		FileInputStream fInputStream = new FileInputStream(fileDir);
		try {
			byte[] bufferArray = new byte[4092];
			int count = 0;
			while ((count = fInputStream.read(bufferArray)) != -1) {
				zOutputStream.write(bufferArray, 0, count);
			}
		} finally {
			fInputStream.close();
		}
		zOutputStream.closeEntry();
    }
    
    private static String computeFilePath(String filePath, String fileName) {
        if (filePath == null || filePath.isEmpty()) {
            return fileName;
        }
        else {
            return filePath + "/" + fileName;
        }
    }

}
