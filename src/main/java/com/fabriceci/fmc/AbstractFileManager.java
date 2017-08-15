package com.fabriceci.fmc;

import com.fabriceci.fmc.error.ClientErrorMessage;
import com.fabriceci.fmc.error.FileManagerException;
import com.fabriceci.fmc.model.*;
import com.fabriceci.fmc.util.FileUtils;
import com.fabriceci.fmc.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractFileManager implements IFileManager {

    protected final static String CONFIG_DEFAULT_PROPERTIES = "filemanager.config.default.properties";
    protected final static String CONFIG_CUSTOM_PROPERTIES = "filemanager.config.properties";
    protected final static String LANG_FILE = "filemanager.lang.en.properties";
    protected final Logger logger = LoggerFactory.getLogger(AbstractFileManager.class);

    protected boolean readOnly = false;

    protected Properties propertiesConfig = new Properties();
    protected Locale locale;

    protected DateFormat df;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public AbstractFileManager(Locale locale, Map<String, String> options) {
        // load server properties
        InputStream tempLoadIS = null;

        // load default config file
        tempLoadIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_DEFAULT_PROPERTIES);
        try {
            propertiesConfig.load(tempLoadIS);
        } catch (IOException ignored) {
        }

        try {
            tempLoadIS.close();
        } catch (IOException ignored) {
        }

        if (locale != null) {
            this.locale = locale;
        }

        // load custom config file if exists
        tempLoadIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_CUSTOM_PROPERTIES);
        if (tempLoadIS != null) {
            Properties customConfig = new Properties();
            try {
                customConfig.load(tempLoadIS);
            } catch (IOException ignored) {
            }

            propertiesConfig.putAll(customConfig);
            try {
                tempLoadIS.close();
            } catch (IOException ignored) {
            }
        }

        try {
            df = new SimpleDateFormat(propertiesConfig.getProperty("dateFormat"), new Locale("en"));
        } catch (IllegalArgumentException e) {
            logger.error("The date format is not valid - setting the default one instead : yyyy-MM-dd HH:mm:ss");
            df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("en"));
        }

        if (options != null && !options.isEmpty()) {
            propertiesConfig.putAll(options);
        }

        readOnly = Boolean.parseBoolean(propertiesConfig.getProperty("readOnly"));
    }

    public AbstractFileManager(Locale locale) {
        this(locale, null);
    }

    public AbstractFileManager(Map<String, String> options) {
        this(null, options);
    }

    public AbstractFileManager() {
        this(null, null);
    }


    public final void handleRequest(HttpServletRequest request, HttpServletResponse response) {

        //baseUrl = ServletUtils.getBaseUrl(request);

        final String method = request.getMethod();
        final String mode = request.getParameter("mode");

        final String pathParam = cleanPath(request.getParameter("path"));

        Object responseData = null;
        response.setStatus(200);

        try {
            if (StringUtils.isEmpty(mode)) {
                generateResponse(response, ClientErrorMessage.MODE_ERROR);
                return;
            }

            if (method.equals("GET")) {
                switch (mode) {
                    default:
                        throw new FileManagerException(ClientErrorMessage.MODE_ERROR);
                    case "initiate":
                        responseData = actionInitiate();
                        break;
                    case "getfile":
                        if (!StringUtils.isEmpty(pathParam)) {
                            responseData = actionGetFile(pathParam);
                        }
                        break;
                    case "getfolder":
                        final String typeParam = request.getParameter("type");
                        if (!StringUtils.isEmpty(pathParam)) {
                            responseData = actionGetFolder(pathParam, typeParam);
                        }
                        break;
                    case "addfolder":
                        final String name = request.getParameter("name");
                        if (!StringUtils.isEmpty(pathParam) && !StringUtils.isEmpty(name)) {
                            responseData = actionAddFolder(pathParam, name);
                        }
                        break;
                    case "move":
                        final String sourcePath = cleanPath(request.getParameter("old"));
                        final String targetPath = cleanPath(request.getParameter("new"));
                        if (!StringUtils.isEmpty(sourcePath) && !StringUtils.isEmpty(targetPath)) {
                            responseData = actionMove(sourcePath, targetPath);
                        }
                        break;
                    case "delete":
                        if (!StringUtils.isEmpty(pathParam)) {
                            responseData = actionDelete(pathParam);
                        }
                        break;
                    case "getimage":
                        if (!StringUtils.isEmpty(pathParam)) {
                            Boolean thumbnail = Boolean.parseBoolean(request.getParameter("thumbnail"));
                            responseData = actionGetImage(response, pathParam, thumbnail);
                        }
                        break;
                }
            }

            if (responseData != null) {
                generateResponse(response, responseData);
            }

        } catch (FileManagerException e) {
            logger.info(e.getMessage(), e);
            generateErrorResponse(response, e.getMessage(), e.getArguments());
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            generateErrorResponse(response, "ERROR_SERVER", null);
        }


    }

    private void generateErrorResponse(HttpServletResponse response, String message, List<String> arguments) {
        response.setStatus(200);
        response.addHeader("Content-Type", "application/json; charset=utf-8");

        Gson gson = new GsonBuilder().create();

        ErrorItem errorItem = new ErrorItem(message, arguments);

        try {
            response.getWriter().write(gson.toJson(new ErrorResponse(errorItem)));
        } catch (IOException silent) {
        }
    }

    private void generateResponse(HttpServletResponse response, Object data) throws IOException {
        response.setStatus(200);
        response.addHeader("Content-Type", "application/json; charset=utf-8");

        Gson gson = new GsonBuilder().create();

        response.getWriter().write(gson.toJson(new SuccessResponse(data)));
    }

    @Override
    public FileData actionGetFile(String path) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> actionGetFolder(String path, String type) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionDownload(HttpServletRequest request, HttpServletResponse response) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionAddFolder(String path, String name) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionDelete(String path) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionRename(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionMove(String sourcePath, String targetPath) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionGetImage(HttpServletResponse response, String path, Boolean thumbnail) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionEditFile(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionSummarize() throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionUpload(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionReplace(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionSaveFile(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionExtract(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InitiateData actionInitiate() throws FileManagerException {

        ConfigUpload configUpload = new ConfigUpload();
        configUpload.setFileSizeLimit(Long.parseLong(propertiesConfig.getProperty("upload.fileSizeLimit")));


        ConfigExtensions configExtensions = new ConfigExtensions();
        boolean policyAllow = Boolean.parseBoolean(propertiesConfig.getProperty("extensions.policy.allow"));
        configExtensions.setPolicy(policyAllow ? "ALLOW_LIST" : "DISALLOW_LIST");
        configExtensions.setRestrictions(propertiesConfig.getProperty("extensions.restrictions").split(","));

        ConfigSecurity configSecurity = new ConfigSecurity();
        configSecurity.setReadOnly(readOnly);
        configSecurity.setExtensions(configExtensions);

        ConfigRoot configRoot = new ConfigRoot();
        configRoot.setSecurity(configSecurity);
        configRoot.setUpload(configUpload);

        InitiateAttributes initiateAttributes = new InitiateAttributes();
        initiateAttributes.setConfig(configRoot);


        InitiateData initiateData = new InitiateData();
        initiateData.setAttributes(initiateAttributes);

        return initiateData;
    }

    @Override
    public FileData actionReadFile(HttpServletRequest request, HttpServletResponse response) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData actionCopy(HttpServletRequest request) throws FileManagerException {
        throw new UnsupportedOperationException();
    }

    protected final boolean isAllowedImageExt(String ext) {
        return Arrays.asList(propertiesConfig.getProperty("images.extensions").split(",")).contains(ext.toLowerCase());
    }

    protected final boolean isAllowedFileExtension(String file) {
        String extension = FileUtils.getExtension(file).toLowerCase();

        boolean policyAllow = Boolean.parseBoolean(propertiesConfig.getProperty("extensions.policy.allow"));
        List<String> restrictions = Arrays.asList(propertiesConfig.getProperty("extensions.restrictions").split(","));

        if (policyAllow) {
            return restrictions.contains(extension);
        } else {
            return !restrictions.contains(extension);
        }
    }

    protected final boolean isAllowedPattern(String name, boolean isDir) throws FileManagerException {

        boolean policyAllow = Boolean.parseBoolean(propertiesConfig.getProperty("patterns.policy.allow"));
        try {
            if (isDir) {
                List<String> restrictionsFolder = Arrays.asList(propertiesConfig.getProperty("patterns.restrictions.folder").split(","));
                boolean isMatch = false;
                for (String regex : restrictionsFolder) {
                    if (name.matches(regex)) isMatch = true;
                }

                return policyAllow == isMatch;

            } else {
                List<String> restrictionsFile = Arrays.asList((propertiesConfig.getProperty("patterns.restrictions.file").split(",")));
                boolean isMatch = false;
                for (String regex : restrictionsFile) {
                    if (name.matches(regex)) isMatch = true;
                }

                return policyAllow == isMatch;
            }
        } catch (PatternSyntaxException e) {
            logger.error("Regex Dir Syntax Exception : " + propertiesConfig.getProperty("excluded_dirs_REGEXP"), e);
            throw new FileManagerException(ClientErrorMessage.ERROR_SERVER);
        }
    }

    protected void checkPath(File file) throws FileManagerException {
        checkPath(file, null);
    }

    protected void checkPath(File file, Boolean isDir) throws FileManagerException {
        if(!file.exists()){
            if(file.isDirectory()){
                throw new FileManagerException(ClientErrorMessage.DIRECTORY_NOT_EXIST, Collections.singletonList(file.getName()));
            } else {
                throw new FileManagerException(ClientErrorMessage.FILE_DOES_NOT_EXIST, Collections.singletonList(file.getName()));
            }
        }

        if(isDir != null){
            if(file.isDirectory() && !isDir){
                throw new FileManagerException(ClientErrorMessage.INVALID_FILE_TYPE);
            }
        }
    }

    protected void checkReadPermission(File file) throws FileManagerException {

        if(!file.canRead()){
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED_SYSTEM);
        }
    }

    protected void checkWritePermission(File file) throws FileManagerException {

        if (readOnly) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED);
        }

        // check system permission
        if (!file.canWrite()) {
            throw new FileManagerException(ClientErrorMessage.NOT_ALLOWED_SYSTEM);
        }
    }

    protected void checkRestrictions(String name, boolean isDir) throws FileManagerException {
        if (!isDir) {
            if (!isAllowedFileExtension(name)) {
                throw new FileManagerException(ClientErrorMessage.FORBIDDEN_NAME, Collections.singletonList(name));
            }
        }

        if (!isAllowedPattern(name, isDir)) {
            throw new FileManagerException(ClientErrorMessage.INVALID_FILE_TYPE, Collections.singletonList(name));
        }
    }

    protected final BufferedImage generateThumbnail(BufferedImage source) {
        return Scalr.resize(source, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_WIDTH, Integer.parseInt(propertiesConfig.getProperty("images.thumbnail.maxWidth")), Integer.parseInt(propertiesConfig.getProperty("images.thumbnail.maxHeight")), Scalr.OP_ANTIALIAS);
    }

    private static String cleanPath(String path) {
        if (path == null) return null;
        return path.replace("//", "/").replace("..", "");
    }


    protected String normalizeName(String input) {

        boolean normalizeFilename = Boolean.parseBoolean(propertiesConfig.getProperty("normalizeFilename"));

        if (!normalizeFilename) return input;

        boolean charsLatinOnly = Boolean.parseBoolean(propertiesConfig.getProperty("charsLatinOnly"));

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);

        return charsLatinOnly ? NONLATIN.matcher(normalized).replaceAll("") : normalized;
    }

    public Properties getPropertiesConfig() {
        return propertiesConfig;
    }

    public void setPropertiesConfig(Properties propertiesConfig) {
        this.propertiesConfig = propertiesConfig;
    }
}