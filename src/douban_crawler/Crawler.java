package douban_crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.Thread;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {
	private final static String commentsCountQuery = "span.pl";
	private final static String titleQuery = "h2 > a";
	private final static String priceInfoQuery = "span.buy-info > a";
	private final static String lineInfoQuery = "div.pub";
	private final static String ratingQuery = "span.rating_nums";
	ArrayList<BookRecord> brs = new ArrayList<BookRecord>();
	ArrayList<String> urlsToBeCrawled = new ArrayList<String>();
	HashMap<String, Integer> depthMap = new HashMap<String, Integer>();
	int page = 0;
	int booksCount = 0;
	int threadCount = 5;
	int waitThreadCount = 0;
	int count = 0;
	int depth = 2;
	String url = "https://book.douban.com/tag/%E7%BC%96%E7%A8%8B?type=S";
	public static final Object signal = new Object();
	
	private void fillUrls(String url, ArrayList<String> urls) {
		try {
			Document doc = Jsoup.connect(url).get();
			Element maxPageEle = doc.select("div.paginator > a").last();
			int maxPage = Integer.parseInt(maxPageEle.text());
			for(int i = 0; i <= maxPage; i++) {
				urls.add(url + "&start=" + i * 20);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Crawler crawler = new Crawler();
			Document doc = Jsoup.connect(crawler.url).get();
			
//			System.out.println(doc.select("div.navigator > a").size());
			Element maxPageEle = doc.select("div.paginator > a").last();
			int maxPage = Integer.parseInt(maxPageEle.text());
			int leastRatingIndex = 0;
			float leastRating = Float.MAX_VALUE;
//			Document doc = Jsoup.parse(new File("C://test.htm"), "UTF8");
			while (crawler.page <= maxPage) {
				String urlCurrent = crawler.url + "&start=" + crawler.page * 20;
				doc = Jsoup.connect(urlCurrent).get();
				for (Element e : doc.getElementsByClass("subject-item")) {
					if (crawler.booksCount >= 40) {
						break;
					}
					String commentsText = e.select(commentsCountQuery).text();
					if(getCommentsCount(commentsText) < 1000) {
						continue;
					}
					crawler.booksCount++;
					BookRecord br = new BookRecord();
					br.setCommentsCount(getCommentsCount(commentsText));
					br.setPriceInfo(e.select(priceInfoQuery).text());
					String lineInfo = (e.select(lineInfoQuery).text());
					parseLineInfo(br, lineInfo);
					br.setRating(Float.parseFloat(e.select(ratingQuery).text()));
					br.setSerial(crawler.booksCount);
					br.setTitle(e.select(titleQuery).attr("title"));
					br.test();
					if (crawler.brs.size() < 40) {
						crawler.brs.add(br);
						for (int i = 0; i < crawler.brs.size(); i++) {
							if (crawler.brs.get(i).getRating() < leastRating) {
								leastRatingIndex = i;
								leastRating = crawler.brs.get(i).getRating();
							}
						}
					} else{
						if (leastRating >= br.getRating()) {
							continue;
						} else {
							crawler.brs.remove(leastRatingIndex);
							crawler.brs.add(br);
						}
					}
				}
				crawler.page++;
				System.out.println(crawler.page);
			}
			Collections.sort(crawler.brs, new SortByRating());
			
			writeExcel("C://test1.xls", crawler.brs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void begin() {
		for (int i = 0; i < threadCount; i++) {
			new Thread(new Runnable(){
				public void run() {
					while (true) {   
//                      System.out.println("当前进入"+Thread.currentThread().getName());  
                        String tmp = getOneUrl();  
                        if(tmp!=null){  
                            crawl(tmp);  
                        }else{  
                            synchronized(signal) {  //------------------（2）  
                                try {  
                                    count++;  
                                    System.out.println(count + " threads waiting");  
                                    signal.wait();  
                                } catch (InterruptedException e) {  
                                    // TODO Auto-generated catch block  
                                    e.printStackTrace();  
                                }  
                            }  
                              
                              
                        }  
                    }  
				}
			},"thread-" + i).start();
		}
	}
	
	private synchronized String getOneUrl() {
		if (urlsToBeCrawled.isEmpty()) {
			return null;		
		}
		String url = urlsToBeCrawled.get(0);
		urlsToBeCrawled.remove(0);
		return url;
	}
	
	private synchronized void addUrl(String url) {
		urlsToBeCrawled.add(url);
		
	}
	
	private void crawl(String url) {
		
	}
	private static int getCommentsCount(String commentsText) {
		String pattern = "\\d+";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(commentsText);
		
		if(m.find() && Integer.parseInt(m.group()) >= 1000) {
			return Integer.parseInt(m.group());
		}
		return 0;
	}
	
	private static void parseLineInfo(BookRecord br, String lineInfo) {
		String[] chunks = lineInfo.split("/");
		
		// If original author is Chinese, then there's no english author, minus one field
		if(chunks.length == 4) {
			br.setAuthorChn(chunks[0]);
			br.setPublisher(chunks[1]);
			br.setCopiedYM(chunks[2]);
			br.setPrice(chunks[3]);
		} else if(chunks.length == 5) {
			br.setAuthorEnu(chunks[0]);
			br.setAuthorChn(chunks[1]);
			br.setPublisher(chunks[2]);
			br.setCopiedYM(chunks[3]);
			br.setPrice(chunks[4]);
		}
	}
	
	private static void writeExcel(String fileName, List<BookRecord> brs) throws IOException, WriteException{
		OutputStream os = new FileOutputStream(new File(fileName));
		WritableWorkbook workbook = Workbook.createWorkbook(os);
		WritableSheet sheet = workbook.createSheet("Douban Top Programming Books", 0);
		
		WritableCellFormat format = new WritableCellFormat();
		format.setAlignment(jxl.format.Alignment.CENTRE);
		format.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);
		
		Label title = new Label(0, 0, "标题");
		Label authorEnu = new Label(1, 0, "英文作者");
		Label authorChn = new Label(2, 0, "中文作者/译者");
		Label publisher = new Label(3, 0, "出版社");
		Label copiedYM = new Label(4, 0, "印刷年月");
		Label price = new Label(5, 0, "价格");
		Label rating = new Label(6, 0, "评分");
		Label commentCount = new Label(7, 0, "评论数");
		sheet.addCell(title);
		sheet.addCell(authorEnu);
		sheet.addCell(authorChn);
		sheet.addCell(publisher);
		sheet.addCell(copiedYM);
		sheet.addCell(price);
		sheet.addCell(rating);
		sheet.addCell(commentCount);
		
		int rowNum = 1;
		for(BookRecord br : brs) {
			Label brTitle = new Label(0, rowNum, br.getTitle());
			Label brAuthorEnu = new Label(1, rowNum, br.getAuthorEnu());
			Label brAuthorChn = new Label(2, rowNum, br.getAuthorChn());
			Label brPublisher = new Label(3, rowNum, br.getPublisher());
			Label brCopiedYM = new Label(4, rowNum, br.getCopiedYM());
			Label brPrice = new Label(5, rowNum, br.getPrice());
			Label brRating = new Label(6, rowNum, br.getRating() + "");
			Label brCommentsCount = new Label(7, rowNum, br.getCommentsCount() + "");
			sheet.addCell(brTitle);
			sheet.addCell(brAuthorEnu);
			sheet.addCell(brAuthorChn);
			sheet.addCell(brPublisher);
			sheet.addCell(brCopiedYM);
			sheet.addCell(brPrice);
			sheet.addCell(brRating);
			sheet.addCell(brCommentsCount);
			rowNum++;
		}
		workbook.write();
		workbook.close();
		os.close();
	}

}
