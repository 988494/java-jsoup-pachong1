package cn.yuping.haha;

import cn.yuping.haha.util.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

@WebServlet(name = "crawlWebServlet", urlPatterns = "/crawlWeb")
public class CrawlWebSite extends HttpServlet {
 
	private final static String charSet = "utf-8";
	private  static String rootDir;
	private  static String rootUrl;			//"http://www.17sucai.com/preview/216556/2016-02-25/%E6%A9%99%E8%89%B2%E5%95%86%E5%9C%BAwap/"

	private final static int timeOut = 30000;

	/**网站上相对地址与绝对地址的映射*/
	private static Map<String,String> absRelativeUrlMap = new HashMap<String,String>();
	/**网站上的url与最终本地使用的url映射*/
	private static Map<String,String> urlmapMap = new HashMap<String,String>();
	/**网站上的css,js*/
	private static Map<String,String> cssjsmapMap = new HashMap<String,String>();
	private static List<File> allFiles = new ArrayList<File>();
	private static Set<String> imgList = new HashSet<String>();
	/**过滤掉不爬取的内容格式*/
//	public static final String filterExtArray []  = {"rar","zip","bmp","dib","gif","jfif","jpe","jpeg","jpg","png","tif","tiff","ico","pdf","doc","docx","xls","xlsx"};
	public static final String filterExtArray []  = {"rar","zip","bmp","dib","jfif","jpe","jpeg","tif","tiff","ico","pdf","doc","docx","xls","xlsx"};

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		 System.out.println("start....");

		 request.setCharacterEncoding("UTF-8");
		 rootUrl = request.getParameter("rootUrl");
		 rootDir = request.getParameter("rootDir");

		HttpSession session = request.getSession();

		if(rootUrl != null && !rootUrl.equals("") && rootDir != null && !rootDir.equals("")){
			if(rootUrl.contains(".html") || rootUrl.contains(".jsp") || rootUrl.contains(".htm")){
				session.setAttribute("msg","网站抓取失败,网址输入有误，不能有‘.html、.jsp’等结尾");
				response.sendRedirect("index.jsp");
				return;
			}

			if(!rootUrl.endsWith("/")){
				rootUrl = rootUrl.concat("/");
			}

			//获取所有urls
			getSubUrls(rootUrl,rootUrl);

			//保存文件
			for(String absUrl : absRelativeUrlMap.keySet()){

				String content;
				try {
					content = readContent(absUrl);
				} catch (IOException e) {
					System.err.println("url3="+absUrl+", 页面无效！");
					continue;
				}
				if(!absUrl.startsWith(rootUrl)){
					continue;
				}
				String filePath = absUrl.substring(rootUrl.length());
				filePath = FileUtils.parseFilePath(filePath);

				//urlmapMap.put(absRelativeUrlMap.get(absUrl), filePath);//脱机运行和在服务器运行有所不同。。。
				urlmapMap.put(absRelativeUrlMap.get(absUrl), rootDir.concat(filePath));//脱机运行。。。
				FileUtils.writeFile(content, rootDir.concat(filePath),charSet);
			}

			System.out.println("--------------------");
			//更新文件中的url
			getAllFiles(new File(rootDir));
			Document doc = null;
			for(File file : allFiles){
				try {
					doc = Jsoup.parse(file, "utf8",rootUrl);
					dealCssJsFile(doc);
					replaceUrl(doc);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String newContent = doc.html();
				FileUtils.writeFile(newContent, file.getAbsolutePath(),charSet);
			}

			for(String absImageUrl:imgList){
				//图片下载之后的路径
				String localCssPath = rootDir.concat(absImageUrl.substring(rootUrl.length()));
				//获取图片全路径之后，下载图片
				FileUtils.downloadFile(absImageUrl,localCssPath);
			}

			System.out.println("finished.");

			destroy();
			session.setAttribute("msg","网站抓取成功，保存地址："+rootDir);
			response.sendRedirect("index.jsp");
		}else{
			session.setAttribute("msg","网站抓取失败，请检查参数是否完整");
			response.sendRedirect("index.jsp");
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	public void destroy() {
		/**网站上相对地址与绝对地址的映射*/
		absRelativeUrlMap = new HashMap<String,String>();
		/**网站上的url与最终本地使用的url映射*/
		urlmapMap = new HashMap<String,String>();
		/**网站上的css,js*/
		cssjsmapMap = new HashMap<String,String>();
		allFiles = new ArrayList<File>();
		imgList = new HashSet<String>();
	}


	/**
	 * 获取指定url页面中的所有链接
	 * @param absUrl
	 * @param relativeUrl
	 * @return
	 * @throws IOException
	 */
	public static void getSubUrls(String absUrl,String relativeUrl)   {
		// TODO Auto-generated method stub

		if(absRelativeUrlMap.get(absUrl)!=null || filter(absUrl)){
			return;
		}
		System.out.println(absUrl);
		absRelativeUrlMap.put(absUrl,relativeUrl);
		Document doc = null;
		try {
			doc = Jsoup.connect(absUrl).get();
		} catch (IOException e) {
			System.err.println("url1="+absUrl+", 页面无效！"+e);
			return;
		}

		Elements eles = doc.body().select("a[href]");

		for(Element ele : eles){
			String absHref = ele.attr("abs:href").replaceAll("\\.\\.\\/", "");
			String href = ele.attr("href");
			if(href.startsWith("javascript") ||
					href.startsWith("#") ||
					(href.contains("(") && href.contains(""))){
				continue;
			}
			if(absHref.startsWith(rootUrl)){
				getSubUrls(absHref,href);
			}
		}
	}

	/**
	 * 读取指定url中的html
	 * @param absUrl
	 * @return
	 * @throws IOException
	 */
	public static String readContent(String absUrl) throws IOException{
		Document doc = Jsoup.connect(absUrl).get();
		//替换图片地址为绝对地址
		for (Element img : doc.body().select("img")) {
			String absImageUrl = img.attr("abs:src");//获得绝对路径
			//获取页面的图片路径，用于后期统一下载
			imgList.add(absImageUrl);
			//图片下载之后的路径
			String localCssPath = rootDir.concat(absImageUrl.substring(rootUrl.length()));
			img.attr("src",localCssPath);
		}
		//替换js地址为绝对地址
		for(Element js : doc.select("script[src]")){
			String absJsUrl = js.attr("abs:src");//获得绝对路径
			//替换地址
			js.attr("src",absJsUrl);
//			System.err.println("js地址为："+absJsUrl);
		}
		//替换css地址为绝对地址
		for(Element css : doc.select("link[href][rel=stylesheet]")){
			String absCssUrl = css.attr("abs:href");//获得绝对路径
			//替换地址
			css.attr("href",absCssUrl);
		}
		return doc.html();
	}


	  //显示目录的方法
	 public static void getAllFiles(File f){
	     //判断传入对象是否为一个文件夹对象
	     if(!f.isDirectory()){
	         System.err.println("你输入的不是一个文件夹，请检查路径是否有误！！");
	     }else{
	         File[] t = f.listFiles();
	         for(int i=0;i<t.length;i++){
	             //判断文件列表中的对象是否为文件夹对象，如果是则执行tree递归，直到把此文件夹中所有文件输出为止
	             if(t[i].isDirectory()){
	            	 getAllFiles(t[i]);
	             }else{
	            	 allFiles.add(t[i].getAbsoluteFile());
	             }
	         }
	     }
	 }


	/**
	 * 替换指定doc中的url
	 * @param doc
	 * @throws IOException
	 */
	public static void replaceUrl(Document doc) {
		Elements eles = doc.body().select("a[href]");
		for(Element ele : eles){
			String href = ele.attr("href");
			String localHref = urlmapMap.get(href);
			if(localHref!=null){
				ele.attr("href",localHref);
			}
		}

	}

	/**
	 * 下载css和js文件，并更新相关链接
	 * @param doc
	 * @throws IOException
	 */
	public static void dealCssJsFile(Document doc) throws Exception {
		//css
        String localCssPath  = null;
		Elements linkEles = doc.select("link[href][rel=stylesheet]");
        int i = 0;
		for(Element ele : linkEles){
			String cssUrl = ele.attr("abs:href");
            if(cssUrl.startsWith(rootUrl)){
                localCssPath = rootDir.concat(cssUrl.substring(rootUrl.length()));
            }else{
                localCssPath = rootDir.concat("linksfile/css/").concat("css_"+i+".css");
            }
			localCssPath = FileUtils.parseFilePath(localCssPath);
			localCssPath = localCssPath.substring(0, localCssPath.lastIndexOf(".css")).concat(".css");
            i ++;
			if(cssjsmapMap.get(localCssPath)!=null){
				ele.attr("href", localCssPath);
				continue;
			}
			cssjsmapMap.put(localCssPath, cssUrl);
			System.out.println(cssUrl);
			//获取css里面的图片，并下载到本地
            FileUtils.cssFile(FileUtils.readFromUrl(cssUrl,charSet),localCssPath,rootUrl,rootDir);
			//下载css文件
			FileUtils.downloadFile(cssUrl,localCssPath);
			ele.attr("href", localCssPath);
		}
		//js
		Elements scriptEles = doc.select("script[src]");
        String localJsPath = null;
        int j = 0;
        for(Element ele : scriptEles){
			String jsUrl = ele.attr("abs:src");//获得绝对路径
            if(jsUrl.startsWith(rootUrl)){
				localJsPath = rootDir.concat(jsUrl.substring(rootUrl.length()));
            }else{
                localJsPath = rootDir.concat("linksfile/js/").concat("js_"+j+".js");
            }
			localJsPath = FileUtils.parseFilePath(localJsPath);
			localJsPath = localJsPath.substring(0, localJsPath.lastIndexOf(".js")).concat(".js");
            j++;
			if(cssjsmapMap.get(localJsPath)!=null){
				ele.attr("src", localJsPath);
				continue;
			}
			cssjsmapMap.put(localJsPath, jsUrl);
			System.out.println(jsUrl);
			//下载js文件
			FileUtils.downloadFile(jsUrl,localJsPath);
			ele.attr("src", localJsPath);
		}
	}
	/**
	 * 在原始的css和js链接上加上绝对地址
	 * @param doc
	 * @throws IOException
	 */
	public static void dealCssJsFile2(Document doc) throws Exception{
		//css
		Elements linkEles = doc.select("link[href]");
		for(Element ele : linkEles){
			String cssUrl = ele.attr("href");//获得绝对路径
			cssUrl = cssUrl.startsWith("/")?cssUrl.substring(1):cssUrl;
			cssUrl = rootUrl.concat(cssUrl.replace(rootUrl, ""));
			ele.attr("href", cssUrl);
			System.out.println(cssUrl);
		}
		//js
		Elements scriptEles = doc.select("script[src]");
		for(Element ele : scriptEles){
			String jsUrl = ele.attr("src");//获得绝对路径
			jsUrl = jsUrl.startsWith("/")?jsUrl.substring(1):jsUrl;
			jsUrl = rootUrl.concat(jsUrl.replace(rootUrl, ""));
			ele.attr("src", jsUrl);
			System.out.println(jsUrl);

		}
	}
	/**
	 * 检查页面中是否存在有效的链接
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static boolean existSubUrls(String url)   {
		// TODO Auto-generated method stub

		boolean exist = false;
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(timeOut).get();
		} catch (IOException e) {
			System.err.println("url2="+url+", 页面无效！");
			return false;
		}

		Elements eles = doc.body().select("a[href]");

		for(Element ele : eles){
			String absHref = ele.attr("abs:href").replaceAll("\\.\\.\\/", "");
			String href = ele.attr("href");
			if(href.startsWith("javascript") ||
					href.startsWith("#") ||
					(href.contains("(") && href.contains(""))){
				continue;
			}
			if(absHref.startsWith("http") && !url.equalsIgnoreCase(absHref)){
				exist = true;
				break;
			}

		}
		return exist;

	}

	public static boolean filter(String url){
		for(String ext : filterExtArray){
			if(FileUtils.isValidFiles(url.toLowerCase(), ext)){
				return true;
			}
		}
		return false;
	}

}
