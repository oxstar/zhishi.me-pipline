package me.zhishi.parser.tools;

import java.io.IOException;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.zhishi.tools.Path;
import me.zhishi.tools.TextTools;
import me.zhishi.tools.URICenter;
import me.zhishi.tools.file.HDFSFileReader;
import me.zhishi.tools.file.NTriplesReader;
import me.zhishi.tools.file.TripleReader;
import me.zhishi.tools.file.TripleWriter;
import me.zhishi.tools.file.ZIPFileWriter;

public class NTStorer
{
	public static double releaseVersion = 3.0;
//	public static String source = URICenter.source_name_baidu;
	public static String source = URICenter.source_name_hudong;
	public static String[] contents = {
//		"label",
//		"category",
//		"abstract",
//		"externalLink",
//		"relatedPage",
//		"internalLink",
//		"redirect",
//		"disambiguation",
//		"articleLink",
//		"image",
		"infobox",
		};
	
	private static ZIPFileWriter writer;
	
	public static void main(String[] args)
	{
//		storeHDFSFile();
//		storeMatches();
//		storeLabels( "category", "categoryLabel" );
//		storeLabels( "infobox", "propertyLabel" );
//		storeHudongSKOS();
	}
	
	public static void storeLabels( String inKey, String outKey )
	{
		Path p = new Path( releaseVersion, source );
		writer = new ZIPFileWriter( p.getNTriplesFolder(), p.getNTriplesFileName( outKey ) );
		NTriplesReader reader = new NTriplesReader( p.getNTriplesFile( inKey ) );
		
		HashSet<String> labelSet = new HashSet<String>();
		while( reader.readNextLine() != null )
		{
			TripleReader tr = reader.getTripleReader();
			String label = null;
			if( inKey.equals( "category" ) )
				label = tr.getObjectContent();
			else if( inKey.equals( "infobox" ) )
				label = tr.getPredicateContent();
			labelSet.add( label );
		}
		
		URICenter uc = new URICenter( source );
		for( String c : labelSet )
		{
			if( inKey.equals( "category" ) )
				writer.writeLine( TripleWriter.getStringValueTriple( uc.getCategoryURI( c ), URICenter.predicate_rdfs_label, c ) );
			else if( inKey.equals( "infobox" ) )
				writer.writeLine( TripleWriter.getStringValueTriple( uc.getURIByKey( "property", c ), URICenter.predicate_rdfs_label, c ) );
		}
		
		reader.close();
		writer.close();
	}
	
	private static HashSet<String> categorySet;
	private static int counter;

	public static void storeHudongSKOS()
	{
		categorySet = new HashSet<String>();
		counter = 0;
		Path p = new Path( releaseVersion, URICenter.source_name_hudong );
		writer = new ZIPFileWriter( p.getNTriplesFolder(), p.getNTriplesFileName( "skosCat" ) );
		
		String root = "页面总分类";
		try
		{
			getSubCategory( root, "1", 0 );
		}
		catch( InterruptedException e )
		{
			e.printStackTrace();
		}
		catch( JSONException e )
		{
			e.printStackTrace();
		}
		
		writer.close();
	}
	
	public static void getSubCategory( String node, String info, int depth ) throws InterruptedException, JSONException
	{
		Document doc = null;
		String UserAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.3 (KHTML, like Gecko) Maxthon/3.3.3.1000 Chrome/16.0.883.0 Safari/535.3";
		String url = "http://www.hudong.com/category/Ajax_cate.jsp?catename=" + TextTools.encoder( node );
		
		for( int i = 0; i <= 100; ++i )
		{
			try
			{
				doc = Jsoup.connect( url ).userAgent( UserAgent ).get();
				break;
			}
			catch( IOException e )
			{
				System.err.println( "IOException, try again. " + node );
				Thread.sleep( 10000 * i );
			}
		}
		
		URICenter uc = new URICenter( URICenter.source_name_hudong );
		
		writer.writeLine( TripleWriter.getStringValueTriple( uc.getCategoryURI( node ), URICenter.predicate_skos_prefLabel, node ) );
		writer.writeLine( TripleWriter.getResourceObjectTriple( uc.getCategoryURI( node ), URICenter.predicate_rdf_type, URICenter.class_skos_concept ) );
		
		JSONArray array = null;
		try
		{
			array = new JSONArray( doc.text() );
		}
		catch( JSONException e )
		{
			return;
		}
		
		for( int i = 0; i < array.length(); ++i )
		{
			String cNode = ((JSONObject) array.get( i )).getString( "name" );
			System.out.println( depth + " " + info + " " +node + " >>> " + cNode );
			
			writer.writeLine( TripleWriter.getResourceObjectTriple( uc.getCategoryURI( node ), URICenter.predicate_skos_broader, uc.getCategoryURI( cNode ) ) );
			writer.writeLine( TripleWriter.getResourceObjectTriple( uc.getCategoryURI( cNode ), URICenter.predicate_skos_narrower, uc.getCategoryURI( node ) ) );
			
			counter++;
			if( categorySet.contains( cNode ) )
				continue;
			categorySet.add( cNode );
			System.err.println( categorySet.size() + "/" + counter );
			getSubCategory( cNode, info + '-' + (i+1), depth + 1 );
		}
	}
	
	public static void storeHDFSFile()
	{
		for( String c : contents )
		{
			Path hp = new Path( releaseVersion, source, true );
			HDFSFileReader hReader = new HDFSFileReader( hp.getNTriplesFile( c ) );
			Path pp = new Path( releaseVersion, source, false );
			ZIPFileWriter zWriter = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( c ) );
			
			System.out.println( "Copying " + pp.getNTriplesFileName( c ) );
			
			String line = null;
			while( (line = hReader.readLine()) != null )
			{
				zWriter.writeLine( line );
			}
			hReader.close();
			zWriter.close();
		}
	}
	
	public static void storeMatches()
	{
		// TODO: version 2.9
		Path pp = new Path( 2.9 );
		pp.setSource( URICenter.source_name_baidu );
		ZIPFileWriter writer1 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "hudongLink" ) );
		ZIPFileWriter writer2 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "zhwikiLink" ) );
		pp.setSource( URICenter.source_name_hudong );
		ZIPFileWriter writer3 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "zhwikiLink" ) );
		ZIPFileWriter writer4 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "baiduLink" ) );
		pp.setSource( URICenter.source_name_zhwiki );
		// TODO: dump version 
		pp.setDumpVersion( "2011" );
		ZIPFileWriter writer5 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "baiduLink" ) );
		ZIPFileWriter writer6 = new ZIPFileWriter( pp.getNTriplesFolder(), pp.getNTriplesFileName( "hudongLink" ) );
		
		Path hp = new Path( releaseVersion, true );
		HDFSFileReader reader = new HDFSFileReader( hp.getMatchingFile() );
		
		String[] ns = { URICenter.namespace_baidu, URICenter.namespace_hudong, URICenter.namespace_zhwiki };
		int count[] = new int[3];
		String line = null;
		while( (line = reader.readLine()) != null )
		{
			String segs[] = line.split( "\t" );
			if( segs[1].contains( ns[0] ) )
			{
				count[0]++;
				writer1.writeLine( TripleWriter.getTripleLine( segs[1], URICenter.predicate_owl_sameAs, segs[2] ) );
				writer4.writeLine( TripleWriter.getTripleLine( segs[2], URICenter.predicate_owl_sameAs, segs[1] ) );
			}
			else if( segs[1].contains( ns[1] ) )
			{
				count[1]++;
				writer3.writeLine( TripleWriter.getTripleLine( segs[1], URICenter.predicate_owl_sameAs, segs[2] ) );
				writer6.writeLine( TripleWriter.getTripleLine( segs[2], URICenter.predicate_owl_sameAs, segs[1] ) );
			}
			else if( segs[1].contains( ns[2] ) )
			{
				count[2]++;
				writer5.writeLine( TripleWriter.getTripleLine( segs[1], URICenter.predicate_owl_sameAs, segs[2] ) );
				writer2.writeLine( TripleWriter.getTripleLine( segs[2], URICenter.predicate_owl_sameAs, segs[1] ) );
			}
		}
		reader.close();

		System.out.println( count[0] );
		System.out.println( count[1] );
		System.out.println( count[2] );
		
		writer1.close();
		writer2.close();
		writer3.close();
		writer4.close();
		writer5.close();
		writer6.close();
	}
}
