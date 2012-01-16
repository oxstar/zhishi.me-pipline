package me.zhishi.parser.tools;

import java.io.IOException;

import me.zhishi.tools.Path;
import me.zhishi.tools.URICenter;
import me.zhishi.tools.file.HDFSFileWriter;
import me.zhishi.tools.file.NTriplesReader;
import me.zhishi.tools.file.TripleReader;

public class ParserTools
{
	public static void main( String[] args ) throws IOException
	{
//		generateBZ2List( URICenter.source_name_hudong );
		superviseNTs( URICenter.source_name_baidu, 3.0, "infobox" );
	}
	
	public static void superviseNTs( String source, double version, String content )
	{
		Path p = new Path( version, source, true );
		NTriplesReader ntReader = new NTriplesReader( p.getNTriplesFile( content ) );
		while( ntReader.readNextLine() != null )
		{
			TripleReader tr = ntReader.getTripleReader();
			String sub = tr.getSubjectContent();
			String pre = tr.getPredicateContent();
			String obj = tr.getObjectValue();
//			if( str.length() >= 16 )
//			if( str.contains( "<" ) )
//			if( str.contains( "[" ) && str.endsWith( "]" ) )
//			if( str.equals( "" ) || str.contains( "null" ) )
			pre = pre.replaceAll( "[：:].*", "" );
			if( obj.equals( "" ) )
				System.out.println( sub + "\t" + pre + "\t" + obj );
		}
	}

	public static void generateBZ2List( String source ) throws IOException
	{
		int maxDump = 0;
		if( source.equals( URICenter.source_name_hudong ) )
			maxDump = Path.hudongMax;
		else if( source.equals( URICenter.source_name_baidu ) )
			maxDump = Path.baiduMax;
		
		Path p = new Path( 3.0, source, true );
		HDFSFileWriter fileWriter = new HDFSFileWriter( p.getMainPageFolder() + "FileList.txt", false );
		
		for( int i = 0; i <= maxDump; ++i )
		{
			String archiveName = Integer.toString( i * 10000 + 1 );
			archiveName += "-";
			archiveName += Integer.toString( (i+1) * 10000 );
			System.out.println( p.getMainPageFolder() + archiveName + ".tar.bz2" );
			fileWriter.writeLine( p.getMainPageFolder() + archiveName + ".tar.bz2" );
		}
		
		fileWriter.close();
	}
}
