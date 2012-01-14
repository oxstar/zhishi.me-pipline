package me.zhishi.tools.file;

import me.zhishi.tools.TextTools;

public class TripleReader
{
	private String triple;
	
	private int SE;
	private int PE;
	private int OE;
	
	public TripleReader( String triple )
	{
		this.triple = triple;
		SE = triple.indexOf( "> <" ) + 1;
		PE = triple.indexOf( "> ", SE + 1 ) + 1;
		OE = triple.lastIndexOf( "." ) -1;
	}
	
	public String getSubject()
	{
		return triple.substring( 0, SE );
	}
	
	public String getSubjectContent()
	{
		String str = getSubject();
		return TextTools.decoder( str.substring( str.lastIndexOf( "/" )+1, str.indexOf( ">" ) ) );
	}
	
	public String getPredicate()
	{
		return triple.substring( SE + 1, PE );
	}
	
	public String getPredicateContent()
	{
		String str = getPredicate();
		return TextTools.decoder( str.substring( str.lastIndexOf( "/" )+1, str.indexOf( ">" ) ) );
	}
	
//	public String getBarePredicate( String start )
//	{
//		String s = getPredicate();
//		int length = start.length();
//		return s.substring( s.indexOf( start )+ length, s.lastIndexOf( ">" ) );
//	}
	
	public String getObject()
	{
		return triple.substring( PE + 1, OE );
	}
	
	public String getObjectContent()
	{
		String str = getObject();
		return TextTools.decoder( str.substring( str.lastIndexOf( "/" )+1, str.indexOf( ">" ) ) );
	}
	
	public String getObjectValue()
	{
		String str = getObject();
		if( str.contains( "\"^^<" ) )
			return TextTools.UnicodeToString(str.substring( 1, str.indexOf( "\"^^<" ) ));
		else if( str.contains( "\"@" ) )
			return TextTools.UnicodeToString(str.substring( 1, str.lastIndexOf( "\"@" ) ));
		else
			return "";
	}
	
	public static void main(String args[])
	{
		TripleReader tr = new TripleReader( "<http://zhishi.me//resource/%E3%80%8A%E4%B8%AD> <http://www.w3.org/2000/01/rdf-schema#label> \"\u300A\u4E2D\"@zh ." );
		System.out.println( tr.getSubject() );
		System.out.println( tr.getPredicate() );
		System.out.println( tr.getObject() );
	}
}