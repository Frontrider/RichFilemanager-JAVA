package com.fabriceci.fmc;

import com.fabriceci.fmc.error.FileManagerException;
import com.fabriceci.fmc.model.FileData;
import com.fabriceci.fmc.model.InitiateData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface IFileManager {

    void handleRequest(HttpServletRequest request, HttpServletResponse response);

    InitiateData actionInitiate() throws FileManagerException;

    FileData actionGetFile(String path) throws FileManagerException;

    List<FileData> actionGetFolder(String path, String type) throws FileManagerException;

    FileData actionReadFile(HttpServletRequest request, HttpServletResponse response) throws FileManagerException;

    FileData actionCopy(HttpServletRequest request) throws FileManagerException;

    FileData actionDownload(HttpServletRequest request, HttpServletResponse response) throws FileManagerException;

    FileData actionAddFolder(String path, String name) throws FileManagerException;

    FileData actionDelete(HttpServletRequest request) throws FileManagerException;

    FileData actionRename(HttpServletRequest request) throws FileManagerException;

    FileData actionMove(HttpServletRequest request) throws FileManagerException;

    FileData actionGetImage(HttpServletRequest request, HttpServletResponse response, Boolean thumbnail) throws FileManagerException;

    FileData actionEditFile(HttpServletRequest request) throws FileManagerException;

    FileData actionSummarize() throws FileManagerException;

    FileData actionUpload(HttpServletRequest request) throws FileManagerException;

    FileData actionReplace(HttpServletRequest request) throws FileManagerException;

    FileData actionSaveFile(HttpServletRequest request) throws FileManagerException;

    FileData actionExtract(HttpServletRequest request) throws FileManagerException;
}
