package me.zhishi.tools;

public class Path
{
	// Poseidon
	public static String projectDataPath = "//POSEIDON/Share/Groups/Semantic Group/Chinese LOD/";
	
	// Hadoop HDFS
	public static String hdfs_username = "APEXLAB-xingniu";
	public static String hdfs_fsName = "hdfs://172.16.7.14";
	public static String hdfs_projectDataPath = "/Users/xingniu/CLOD/";
	private static boolean isHDFS = false;
	
	public static String baiduFileName = "baidubaike";
	public static String hudongFileName = "hudongbaike";
	public static String zhwikiFileName = "zhwiki";
	
	private String source;
	private String sourcefileName;
	private double releaseVersion;
	
	private String dumpVersion;
	private String dumpPath;
	
	public static int hudongMax = 364;
	public static int baiduMax = 714;
	
	public void setSourcefileName( String source )
	{
		if( source.equals( URICenter.source_name_baidu ) )
			sourcefileName = baiduFileName;
		else if( source.equals( URICenter.source_name_hudong ) )
			sourcefileName = hudongFileName;
		else if( source.equals( URICenter.source_name_zhwiki ) )
			sourcefileName = zhwikiFileName;
	}
	
	public void init( double releaseVersion )
	{
		this.releaseVersion = releaseVersion;
		
		String dumpVersion = null;
		if( releaseVersion >= 2.0 && releaseVersion < 3.0 )
			dumpVersion = "2011";
		else if( releaseVersion >= 3.0 && releaseVersion < 4.0 )
			dumpVersion = "2011.12";
		this.dumpVersion = dumpVersion;
		
		dumpPath = projectDataPath + dumpVersion + "/";	
	}
	
	public Path( double releaseVersion )
	{
		init( releaseVersion );
	}
	
	public Path( double releaseVersion, String source )
	{
		this( releaseVersion );
		this.source = source;
		setSourcefileName( source );
	}
	
	public Path( double releaseVersion, String source, boolean isHDFS )
	{
		this( releaseVersion, source );
		this.isHDFS = isHDFS;
	}
	
//	public Path( String dumpVersion )
//	{
//		this.dumpVersion = dumpVersion;
//		dumpPath = projectDataPath + dumpVersion + "/";
//	}
//	
//	public Path( String dumpVersion, String source  )
//	{
//		this( dumpVersion );
//		this.source = source;
//		setSourcefileName( source );
//	}
	
	public void setDumpVersion( String newVersion )
	{
		dumpVersion = newVersion;
		dumpPath = projectDataPath + dumpVersion + "/";
	}
	
	public String getDumpVersion()
	{
		return dumpVersion;
	}
	
	public void setReleaseVersion( double newVersion )
	{
		releaseVersion = newVersion;
	}
	
	public double getReleaseVersion()
	{
		return releaseVersion;
	}
	
	public void setSource( String src )
	{
		source = src;
		setSourcefileName( src );
	}
	
	public String getSource()
	{
		return source;
	}
	
	public String getMainPagePath()
	{
		return dumpPath + source + "/MainPages/";
	}
	
	public String getMainPageFilePath( String file )
	{
		return getMainPagePath() + file + ".tar.bz2";
	}
	
	public String getHDFSMainPagePath()
	{
		return hdfs_projectDataPath + "BaikePages/" + source + "/";
	}
	
	public String getHDFSRawStructuredDataPath()
	{
		return hdfs_projectDataPath + "RawStructuredData/" + source + "/";
	}
	
	public String getNTriplesPath()
	{
		if( isHDFS )
			return hdfs_projectDataPath + "NTriples/";
		else
			return dumpPath + source + "/NTriples/";
	}
	
	public String getAbstractFileName()
	{
		return getNTriplesPath()+releaseVersion+"_"+sourcefileName+"_abstracts_zh.nt";
	}
	
	public String getLabelFileName()
	{
		return getNTriplesPath()+releaseVersion+"_"+sourcefileName+"_labels_zh.nt";
	}
	
	public String getCategoryFileName()
	{
		return getNTriplesPath()+releaseVersion+"_"+sourcefileName+"_article_categories_zh.nt";
	}
}
