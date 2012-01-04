package me.zhishi.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import me.zhishi.tools.URICenter;
import me.zhishi.tools.StringPair;

public class BaiduParser implements ZhishiParser
{
	public static void main( String[] args ) throws IOException
	{
		String url = "http://baike.baidu.com/view/169.htm";
		BaiduParser p = new BaiduParser( url );
		p.parse();
	}
	
	private Document doc;
	
	public BaiduParser( InputStream is ) throws IOException
	{
		doc = Jsoup.parse( is, "GB18030", "http://baike.baidu.com" );
	}

	public BaiduParser( String url ) throws IOException
	{
		doc = Jsoup.connect( url ).get();
	}

	@Override
	public Article parse()
	{
		ZhishiArticle article = new ZhishiArticle( URICenter.source_name_baidu );
		article.label = getLabel();
		article.categories = getCategories();
		article.abs = getAbstract();
		article.isRedirect = isRedirectPage(); 
		article.redirect = getRedirect();
		article.relatedPages = getRelatedLabels();
		article.pictures = getPictures();
		article.properties = getProperties();
		article.internalLinks = getInternalLinks();
		article.externalLinks = getExternalLinks();
		article.isDisambiguationPage = isDisambiguationPage();
		article.disambiguationLabels = getDisambiguations();
		if (article.isDisambiguationPage)
			article.disambiguationArticles = BaiduDisParse();
		return article;
	}

	@Override
	public String getLabel()
	{
		String label = doc.select("h1[class=title]").html();
		label = StringEscapeUtils.unescapeHtml4(label);
		label = label.trim();
//		System.out.println( label );
//		System.out.println( doc.select("title").text() );
		return label;
	}

	@Override
	public String getAbstract()
	{
		return (doc.select("div[class*=card-summary]").select("p").text()).replace(whitespace, "");
	}
	
	@Override
	public boolean isRedirectPage(){
		return getRedirect() != null;
	}
	
	@Override
	public String getRedirect()
	{
		String redirect = null;
		if (!doc.select("div[class^=view-tip-pannel]").select("a[href$=redirect]").isEmpty())
			redirect = doc.select("div[class^=view-tip-pannel]").select("a[href$=redirect]").text();

		if (!doc.select("div[class^=view-tip-pannel]").select("a[class$=synstd]").isEmpty()) 
			redirect = doc.select("div[class^=view-tip-pannel]").select("a[href*=history]").text();
		
		if (redirect!=null){
			if (redirect.contains(getLabel()) && isDisambiguationPage()) 
				return null;
			redirect = StringEscapeUtils.unescapeHtml4(redirect);
			System.out.println( redirect );
			if (redirect.length() > 80) return "";
		}
		return redirect;
	}

	@Override
	public ArrayList<StringPair> getPictures()
	{
		ArrayList <StringPair> pics = new ArrayList<StringPair>();
		
		for(Element img:doc.select("div[class*=main-body]").select("img"))
			if(img.hasAttr("title")) {
				String picTitle = img.attr("title");
				if (picTitle.length() == 0)
					picTitle = getLabel();
				picTitle = picTitle.replaceAll(whitespace, "");
				picTitle = picTitle.trim();
				pics.add(new StringPair(img.attr("src"), picTitle));
			}
		
		return pics;
	}

	@Override
	public ArrayList<StringPair> getProperties()
	{
		ArrayList <StringPair> properties = new ArrayList<StringPair>();
		
		for(Element e:doc.select("div[class*=card-info] td[class=cardFirstTd")){
			StringPair p = new StringPair();
			p.first = e.text();
			if (p.first.contains("："))
				p.first = p.first.substring(0, p.first.indexOf("："));
			properties.add(p);
		}
		int i = 0;
		for(Element e:doc.select("div[class*=card-info] td[class=cardSecondTd")){
			properties.get(i).second = e.text();
			++i;
		}
		
		return properties;
	}

	@Override
	public ArrayList<String> getCategories()
	{
		ArrayList<String> categories = new ArrayList<String>();
		
		for( Element cat : doc.select( "dl#viewExtCati > dd > a" ) )
			categories.add( cat.text() );
		
		return categories;
	}

	@Override
	public ArrayList<String> getInternalLinks()
	{
		ArrayList<String> innerLinks = new ArrayList<String>();
		
		for (Element link: doc.select("a[href^=/view/] , a[href^=http://baike.baidu.com/view/"))
			if(link.hasAttr("href") && link.attr("href").endsWith("htm")) {
				if (link.parent().hasAttr("class") && link.parent().attr("class").endsWith("cardSecondTd")) {
				} else {
					innerLinks.add( StringEscapeUtils.unescapeHtml4(link.text()));
				}
			}
		
		return innerLinks;
	}

	@Override
	public ArrayList<String> getExternalLinks()
	{
		ArrayList<String> outerLinks = new ArrayList<String>();
		
		for (Element link: doc.select("div[class*=main-body]").select("a"))
			if (link.hasAttr("href")){
				String tmp = link.attr("href");
				if (tmp.startsWith("http://") && !tmp.startsWith("http://baike.baidu.com/view/"))
					outerLinks.add(tmp);
			}
		
		return outerLinks;
	}

	@Override
	public ArrayList<String> getRelatedLabels()
	{
		ArrayList<String> relatedLabels = new ArrayList<String>();
		for ( Element relat: doc.select( "dl#relatedLemmaDown > dd" ).select("a") )
			if ( relat.hasAttr("href") && relat.attr("href").startsWith("/view/") ) 
				relatedLabels.add( StringEscapeUtils.unescapeHtml4(relat.text()) );
		
		return relatedLabels;	
	}

	@Override
	public boolean isDisambiguationPage()
	{
		return !doc.select("div[class*=nslog:517]").isEmpty();
	}
	
	@Override
	public ArrayList<String> getDisambiguations()
	{
		ArrayList<String> disambiguations = new ArrayList<String>();
		
		for (Element link :doc.select("ol[data-nslog-type=503] > li > a")) {
			String tmp = getLabel() + "[" + link.text() + "]";
			disambiguations.add(tmp);	
		}
		
//		for (String s : disambiguations)
//			System.out.println(s);
		return disambiguations;
	}
	
	public ArrayList<Article> BaiduDisParse()
	{
		ArrayList<Article> disArticles = new ArrayList<Article>();
		//To-Do
		return disArticles;
	}
}
