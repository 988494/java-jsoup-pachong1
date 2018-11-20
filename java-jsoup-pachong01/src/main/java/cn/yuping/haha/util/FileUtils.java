package cn.yuping.haha.util;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


/**
 * 文件工具类
 */
public class FileUtils {
    public static byte[] readInputStreamToByteArray(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            int len;
            byte[] buf = new byte[512];
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        } finally {
            outputStream.close();
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * 判断某个文件是否属于某些格式文件
     *
     * @param fileName 文件地址
     * @param exts     有效的文件扩展名，如"jpg;.jpeg;.png"
     */
    public static Boolean isValidFiles(String fileName, String exts) {
        if (StringUtil.isNullOrEmpty(fileName)) {
            return false;
        }

        String ext = getFileExtensionName(fileName);
        if (!StringUtil.isNullOrEmpty(ext)) {
            String[] extArray = exts.split(";");
            for (String e : extArray) {
                if (ext.equalsIgnoreCase(e)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * 获取文件的扩展名
     *
     * @param fileName 文件名，可带路径
     * @return 返回带文件扩展名，如"jpg"，如果没有扩展名则返回空字符值
     */
    public static String getFileExtensionName(String fileName) {
        if (StringUtil.isNullOrEmpty(fileName)) {
            return "";
        }
        int p = fileName.lastIndexOf('.');
        if (p != -1) {
            return fileName.substring(p + 1);
        }
        return "";
    }

    /**
     * 获取URL文件名
     */
    public static String getUrlFileName(String url) {

        // 去除"?"后面的参数
        String fileNameArr[] = url.split("\\?");
        if (fileNameArr != null && fileNameArr.length > 0) {
            // 获取"/"后的文件名
            return fileNameArr[0]
                    .substring(fileNameArr[0].lastIndexOf('/') + 1);
        } else {
            // 获取"/"后的文件名
            return url.substring(url.lastIndexOf('/') + 1);
        }
    }

    public static String readInputStreamToString(InputStream inputStream,
                                                 String charset) throws IOException {
        return new String(readInputStreamToByteArray(inputStream), charset);
    }

    public static boolean isWriting(File file) {
        return file.lastModified() / 1000 >= System.currentTimeMillis() / 1000;
    }

    /**
     * 功能描述：根据路径名创建目录
     *
     * @param filePath 路径名
     * @return 当创建目录成功后，返回true,否则返回false.
     */
    public static boolean createDirectory(String filePath) {
        boolean isDone = false;
        File file = new File(filePath);
        if (file.exists())
            return true;
        isDone = file.mkdirs();
        return isDone;
    }

    /**
     * 转换文件名
     *
     * @param filePath
     * @return
     */
    public static String parseFilePath(String filePath) {
        File file = new File(filePath);
        String fileName = getUrlFileName(file.getName());
        String extName = getFileExtensionName(fileName);
        if ("".equals(fileName) || "/".equals(fileName)) {
            fileName = "index.html";
        } else if (StringUtil.isNullOrEmpty(extName)) {
            fileName = new java.util.Date().getTime() + ".html";
            File other = null;
            if (StringUtil.isNullOrEmpty(file.getParent())) {
                other = new File("others");
            } else {
                other = new File(file.getParent().concat(File.separator).concat("others"));
            }
            if (!other.exists()) {
                other.mkdirs();
            }
            fileName = "others".concat(File.separator).concat(new java.util.Date().getTime() + ".html");
        } else {
            fileName = URLEncoder.encode(fileName);
        }
        String path = "";
        if (StringUtil.isNullOrEmpty(file.getParent())) {
            path = fileName;
        } else {
            path = file.getParent().concat(File.separator).concat(fileName);
        }

        return path;
    }

    /**
     * 获取css里面的图片地址
     */
    public static String cssFile(String str, String outFile, String rootUrl, String rootDir) throws Exception {
        Map<String, String> cssImages = new HashMap<String, String>();
        String localCssPath = null;
        if (outFile.endsWith("css")) {
            //用正则表达式，截取以"background:url("开头，以")"结尾的1~50和字符串
            Pattern p = Pattern.compile("(?<=(background:url\\()).{1,50}(?=(\\)))");
            Matcher m = p.matcher(str);
            while (m.find()) {
                String imgs = m.group().replaceAll("\\.\\.\\/", "");        //获取图片的绝对路径
                imgs = imgs.startsWith("/") ? imgs.substring(1) : imgs;
                imgs = rootUrl.concat(imgs.replace(rootUrl, ""));
                localCssPath = rootDir.concat("linksfile/").concat(imgs.substring(rootUrl.length()));       //拼接当前本地路径
                localCssPath = FileUtils.parseFilePath(localCssPath);
                //下载图片到指定本地路径
                downloadImg(imgs, localCssPath);

//                str = str.replace(m.group(),"../"+imgs);        //替换图片地址
                if (cssImages.get(localCssPath) != null) {
//                    str = str.replace(m.group(),"../"+imgs);
                    continue;
                }
                cssImages.put(localCssPath, imgs);
            }
        }
        return str;
    }


    /**
     * 下载图片到本地
     */
    public static void downloadImg(String imgUrl, String localCssPath) {
        try {
            URL url = new URL(imgUrl);
            DataInputStream dataInputStream = new DataInputStream(url.openStream());

            String imageName = localCssPath;
            File file = new File(imageName);
            if (file.exists()) {
                file.delete();
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(imageName);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = dataInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            byte[] context = output.toByteArray();
            fileOutputStream.write(output.toByteArray());
            dataInputStream.close();
            fileOutputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输出String到文件
     */
    public static void writeFile(String str, String outFile, String charSet) {
        byte[] buff = new byte[]{};
        FileOutputStream out = null;
        try {
            buff = str.getBytes(charSet);
            File file = new File(outFile);
            if (file.exists()) {
                file.delete();
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            out = new FileOutputStream(outFile);
            out.write(buff, 0, buff.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    out = null;
                }
            }
        }
    }




    /**
     * 根据url地址，直接下载文件
     * @param urlStr
     * @return
     */
    public static void downloadFile(String urlStr, String outFile) {

        URL url = null;
        try {

            url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(30000);
            InputStream inputStream = conn.getInputStream();
            File file = new File(outFile);
            if (file.exists()) {
                file.delete();
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            byte[] b = new byte[1024];
            int n=0;
            while((n=inputStream.read(b))!=-1){
                fos.write(b, 0, n);
            }
            inputStream.close();
            fos.close();


            //URL server = new URL("http://203.98.189.57/images/product_images/Astrid%20Wallet_main.jpg");
//            url = new URL(urlStr);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setDoInput(true);
//            connection.setDoOutput(true);
//            connection.setUseCaches(false);
//            connection.addRequestProperty("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/x-shockwave-flash, */*");
//            connection.addRequestProperty("Accept-Language", "en-us,zh-cn;q=0.5");
//            connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
//            connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; .NET CLR 2.0.50727; MS-RTC LM 8)");
//            connection.connect();
//            InputStream is = connection.getInputStream();
//
//            Map map = connection.getHeaderFields();
//            Iterator it = map.keySet().iterator();
//
//            File file = new File(outFile);
//            if (file.exists()) {
//                file.delete();
//            }
//            if (!file.getParentFile().exists()) {
//                file.getParentFile().mkdirs();
//            }
//
//            boolean gzip = false;
//            while(it.hasNext()) {
//                Object type =  map.get(it.next());
//                if(type.toString().indexOf("gzip") != -1){
//                    gzip = true;
//                    break;
//                }
//            }
//
//            OutputStream os = new FileOutputStream(outFile);
//            byte[] buffer = new byte[1024];
//
//            if(!gzip){
//                int byteReaded = is.read(buffer);
//                while (byteReaded != -1) {
//                    os.write(buffer, 0, byteReaded);
//                    byteReaded = is.read(buffer);
//                }
//            }else{
//                GZIPInputStream gis = new GZIPInputStream(is);
//                int byteReaded = 0;
//                while ((byteReaded = gis.read(buffer)) != -1) {
//                    os.write(buffer, 0, byteReaded);
//                }
//            }
//            os.close();
//            connection.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO异常：" + e);
        }
    }




    /**
     * 根据字符串，写入文件
     * @param urlStr
     * @return
     */
    public static String readFromUrl(String urlStr, String charSet) {

        URL url = null;
        String readString;

        StringBuffer sb = new StringBuffer();
        try {
            url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(30000);
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charSet));
            while ((readString = br.readLine()) != null) {
                sb.append(readString);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO异常：" + e);
        }
        return sb.toString();
    }

}
