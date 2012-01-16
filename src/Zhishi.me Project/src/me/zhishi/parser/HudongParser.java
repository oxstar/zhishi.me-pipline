package me.zhishi.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import me.zhishi.parser.ZhishiArticle.ImageInfo;
import me.zhishi.tools.TextTools;
import me.zhishi.tools.URICenter;
import me.zhishi.tools.StringPair;

public class HudongParser implements ZhishiParser
{
	public static void main(String args[]) throws IOException
	{
		String url = "http://www.hudong.com/wiki/上海市";
		HudongParser p = new HudongParser( url );
		Article article = p.parse();
		
		for( String t : article.toTriples() )
		{
			System.out.println( t );
		}
	}
	
	protected Document doc;
	
	public HudongParser( InputStream is, String fileName ) throws IOException
	{
		doc = Jsoup.parse( is, "UTF-8", "http://www.hudong.com/" );
	}
	
	public HudongParser( String url ) throws IOException
	{
		doc = Jsoup.connect( url ).get();
	}
	
	@Override
	public Article parse()
	{
		ZhishiArticle article = new ZhishiArticle( URICenter.source_name_hudong );
		
		article.isRedirect = isRedirectPage();
		if( article.isRedirect )
		{
			article.label = getRedirect();
			if( article.label != null )
				article.articleLink = "http://www.hudong.com/wiki/" + TextTools.encoder( article.label );
			article.redirect = getLabel();
			return article;
		}
		else
		{
			article.label = getLabel();
			if( article.label != null )
				article.articleLink = "http://www.hudong.com/wiki/" + TextTools.encoder( article.label );
			article.redirect = getRedirect();
		}
		
		article.isDisambiguationPage = isDisambiguationPage();
		article.disambiguationLabels = getDisambiguations();
		if( article.isDisambiguationPage )
			return article;
		
		article.abs = getAbstract();
		article.categories = getCategories();
		article.relatedPages = getRelatedPages();
		article.imageInfo = getImageInfo();
		article.properties = getProperties();
		article.internalLinks = getInternalLinks();
		article.externalLinks = getExternalLinks();
		return article;
	}

	@Override
	public String getLabel()
	{
		String label = null;
		if( !isDisambiguationPage() )
		{
			label = doc.select( "div[class^=content-h1]" ).select( "h1" ).text();
		}
		else
		{
			for( Element e : doc.select( "div[class=prompt] > p > a" ) )
				if( e.hasAttr( "href" ) && e.attr( "href" ).contains( "/wiki/" ) )
				{
					label = e.text();
					if( label.contains( "[" ) )
						label = label.substring( 0, label.indexOf( "[" ) );
					break;
				}
		}
		label = StringEscapeUtils.unescapeHtml4(label);
		label = label.trim();
		if( !label.equals( "" ) )
			return label;
		else
			return null;
	}

	@Override
	public String getAbstract()
	{
		String abs = doc.select("div[class=summary] > p").text().replaceAll( whitespace, "" );
		if( abs.startsWith( "请用一段简单的话描述该词条" ) || abs.equals( "" ) )
			return null;
		else
			return abs;
	}

	@Override
	public boolean isRedirectPage()
	{
		String tmp = doc.select( "div[id=unifyprompt] > p[id=unifypromptone]" ).text();
		return tmp.contains( "是" ) && tmp.substring( tmp.indexOf( "是" ) ).contains( "的同义词" );
	}
	
	@Override
	public String getRedirect()
	{
		if( isRedirectPage() )
		{
			for( Element re : doc.select( "div[id=unifyprompt] > p[id=unifypromptone] > a" ) )
			{
				String redirect = re.text();
				if( !re.attr( "href" ).contains( "wiki/" ) || redirect.equals( "" ) )
					return null;
				return redirect;
			}
		}
		return null;
	}
	
	private String getCompleteImg( String img )
	{
		return img.replaceAll( "_.*?\\.", "." );
	}
	
	private String getCleanImgTitle( Element img )
	{
		String title = img.attr( "title" );
		if( title.equals( "null" ) )
			title = img.attr( "alt" );
		if( title.equals( "null" ) )
			return "";
		if( title.startsWith( "（图）" ) )
			title = title.replaceAll( "（图）", "" );
		title = title.replaceAll( whitespace, "" );
		return title;
	}
	
	@Override
	public ImageInfo getImageInfo()
	{
		ImageInfo imageInfo = new ImageInfo();
		
		for( Element pic : doc.select( "div[id=docinfotemplettable] td[class=a-c p-tb10] > a" ) )
		{
			if( !pic.children().toString().equals( "" ) )
			{
				if( !pic.child( 0 ).attr( "src" ).contains( ".att.hudong.com" ) )
					continue;
				imageInfo.depictionThumbnail = pic.child( 0 ).attr( "src" );
				imageInfo.depiction = getCompleteImg( imageInfo.depictionThumbnail );
				imageInfo.labels.add( new StringPair( imageInfo.depiction, getLabel() ) );
				imageInfo.rights.add( new StringPair( imageInfo.depiction, pic.attr( "href" ) ) );
				imageInfo.thumbnails.add( new StringPair( imageInfo.depiction, imageInfo.depictionThumbnail ) );
			}
		}

		for( Element pic : doc.select( "div[class=summary] > div[class=img img_r] > a" ) )
		{
			if( !pic.children().toString().equals( "" ) )
			{
				Element img = pic.child( 0 );
				String imgURI = getCompleteImg( img.attr( "src" ) );
				if( !imgURI.contains( ".att.hudong.com" ) )
					continue;
				if( imageInfo.depiction == null )
				{
					imageInfo.depictionThumbnail = img.attr( "src" );
					imageInfo.depiction = imgURI;
				}
				else
				{
					imageInfo.relatedImages.add( imgURI );
				}
				imageInfo.labels.add( new StringPair( imgURI, getCleanImgTitle( img ) ) );
				imageInfo.rights.add( new StringPair( imgURI, pic.attr( "href" ) ) );
				imageInfo.thumbnails.add( new StringPair( imgURI, img.attr( "src" ) ) );
			}
		}
		
		for( Element pic : doc.select( "div[id=content] > div[class=img img_r] > a" ) )
		{
			if( !pic.children().toString().equals( "" ) )
			{
				Element img = pic.child( 0 );
				String imgURI = getCompleteImg( img.attr( "src" ) );
				imageInfo.relatedImages.add( imgURI );
				if( !imgURI.contains( ".att.hudong.com" ) )
					continue;
				imageInfo.labels.add( new StringPair( imgURI, getCleanImgTitle( img ) ) );
				imageInfo.rights.add( new StringPair( imgURI, pic.attr( "href" ) ) );
				imageInfo.thumbnails.add( new StringPair( imgURI, img.attr( "src" ) ) );
			}
		}
		
		return imageInfo;
	}

	@Override
	public ArrayList<StringPair> getProperties()
	{
		ArrayList <StringPair> properties = new ArrayList<StringPair>();
		
		for( Element infoBox : doc.select( "div[id=docinfotemplettable]" ) )
		{
			for( Element tr : infoBox.select( "tr" ) )
			{
				StringPair pv = new StringPair();
				boolean hasProperty = false;
				boolean hasValue = false;
				for( Element e : tr.select( "td" ) )
				{
					if( e.hasAttr( "align" ) )
					{
						hasProperty = true;
						pv.first = e.text();
						pv.first = pv.first.replaceAll( "[：:].*", "" );
					}
					if( e.hasAttr( "style" ) )
					{
						hasValue = true;
						pv.second = e.text();
					}
				}
				if( hasProperty && hasValue )
				{
					properties.add( pv );
				}
			}
		}
		
		return properties;
	}

	@Override
	public ArrayList<String> getCategories()
	{
		ArrayList<String> categories = new ArrayList<String>();
		
		for( Element cat : doc.select( "div[class=relevantinfo] > dl[id=show_tag] > dd > a" ) )
			if( cat.hasAttr( "href" ) && cat.attr( "href" ).contains( "/categorypage" ) )
				categories.add( cat.text() );
		
		return categories;
	}

	@Override
	public ArrayList<String> getInternalLinks()
	{
		HashSet<String> internalLinksSet = new HashSet<String>();
		
		for( Element e : doc.select( "a[class=innerlink]" ) )
		{
			if( !e.attr( "title" ).equals( "" ) && !e.text().equals( "" ) && !e.parents().hasClass( "table" ) )
				internalLinksSet.add( e.text() );
		}
		
		return new ArrayList<String>( internalLinksSet );
	}

	@Override
	public ArrayList<String> getExternalLinks()
	{
		ArrayList<String> outerLinks = new ArrayList<String>();

		for( Element link : doc.select( "div[class=relevantinfo] > dl[class^=reference]" ).select( "input" ) )
			if( link.hasAttr( "value" ) )
				outerLinks.add( link.attr( "value" ).replaceAll( "[\\s]", "" ) );
		for( Element link : doc.select( "div[class=relevantinfo] > dl[id=show_quote]" ).select( "a" ) )
			if( link.hasAttr( "href" ) )
			{
				String tmp = link.attr( "href" );
				if( tmp.startsWith( "http://" ) )
					outerLinks.add( tmp.replaceAll( "[\\s]", "" ) );
			}

		return outerLinks;
	}

	@Override
	public ArrayList<String> getRelatedPages()
	{
		ArrayList<String> relatedPages = new ArrayList<String>();
		for( Element relat : doc.select( "div[class^=xgct] > ul" ).select( "a" ) )
			if( relat.hasAttr( "href" ) && relat.attr( "href" ).startsWith( "/wiki/" ) && !relat.attr( "title" ).equals( "" ) )
				relatedPages.add( relat.attr( "title" ) );
		
		return relatedPages;
	}

	@Override
	public boolean isDisambiguationPage()
	{
		return !doc.select( "dl[class=polysemy]" ).isEmpty();
	}
	
	@Override
	public ArrayList<String> getDisambiguations()
	{
		ArrayList<String> disambiguations = new ArrayList<String>();
		
		for( Element e : doc.select( "div[class=prompt] > p > a" ) )
			if( e.hasAttr( "href" ) && e.attr( "href" ).contains( "/wiki/" ) && !e.text().equals( "" ) )
				disambiguations.add( e.text() );

		return disambiguations;
	}
}
