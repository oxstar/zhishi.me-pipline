package me.zhishi.parser.workshop;

import java.io.IOException;
import java.util.HashSet;

import me.zhishi.tools.SmallTools;
import me.zhishi.tools.URICenter;
import me.zhishi.tools.file.TripleReader;
import me.zhishi.tools.file.TripleWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class SortByPredicate
{
	private static int numReduceTasks = 20;
	
	private static HashSet<String> contents = new HashSet<String>();
	static
	{
		contents.add( "label" );
		contents.add( "category" );
		contents.add( "abstract" );
		contents.add( "relatedPage" );
		contents.add( "internalLink" );
		contents.add( "externalLink" );
		contents.add( "redirect" );
		contents.add( "disambiguation" );
		contents.add( "articleLink" );
		contents.add( "image" );
		contents.add( "imageInfo" );
		contents.add( "infoboxText" );
		contents.add( "exception" );
	}
	
	public static class SortByPredicateMapper extends Mapper<Object, Text, Text, Text>
	{
		public void map( Object key, Text value, Context context ) throws IOException, InterruptedException
		{
			TripleReader tr = new TripleReader( value.toString() );
			context.write( new Text( tr.getSubject() ), value );
		}
	}
	
	public static class SortByPredicateReducer extends Reducer<Object, Text, NullWritable, Text>
	{
		private MultipleOutputs<NullWritable, Text> mos;
		
		@Override
		protected void setup( Context context ) throws IOException, InterruptedException
		{
			super.setup( context );
			mos = new MultipleOutputs<NullWritable, Text>( context );
		}

		@Override
		public void reduce( Object key, Iterable<Text> values, Context context ) throws IOException, InterruptedException
		{
			HashSet<String> tripleSet = new HashSet<String>();
			for( Text val : values )
			{
				String triple = val.toString();
				if( tripleSet.contains( triple ) )
					continue;
				else
					tripleSet.add( triple );
				TripleReader tr = new TripleReader( triple );
				String pre = tr.getBarePredicate();
				if( contents.contains( "label" ) && pre.equals( URICenter.predicate_rdfs_label ) && tr.getBareSubject().startsWith( URICenter.domainName ) )
					mos.write( "label", NullWritable.get(), val );
				else if( contents.contains( "category" ) && pre.equals( URICenter.predicate_category ) )
					mos.write( "category", NullWritable.get(), val );
				else if( contents.contains( "abstract" ) && pre.equals( URICenter.predicate_abstract ) )
					mos.write( "abstract", NullWritable.get(), val );
				else if( contents.contains( "relatedPage" ) && pre.equals( URICenter.predicate_relatedPage ) )
					mos.write( "relatedPage", NullWritable.get(), val );
				else if( contents.contains( "internalLink" ) && pre.equals( URICenter.predicate_internalLink ) )
					mos.write( "internalLink", NullWritable.get(), val );
				else if( contents.contains( "externalLink" ) && pre.equals( URICenter.predicate_externalLink ) )
					mos.write( "externalLink", NullWritable.get(), val );
				else if( contents.contains( "redirect" ) && pre.equals( URICenter.predicate_redirect ) )
					mos.write( "redirect", NullWritable.get(), val );
				else if( contents.contains( "disambiguation" ) && pre.equals( URICenter.predicate_disambiguation ) )
					mos.write( "disambiguation", NullWritable.get(), val );
				else if( contents.contains( "articleLink" ) && pre.equals( URICenter.predicate_foaf_isPrimaryTopicOf ) )
				{
					String resource = tr.getBareSubject();
					String articleLink = tr.getBareObject();
					Text pt = new Text( TripleWriter.getResourceObjectTriple( articleLink, URICenter.predicate_foaf_primaryTopic, resource ) );
					Text lang = new Text( TripleWriter.getStringValueTripleAT( articleLink, URICenter.predicate_dc_language, "zh", "en" ) );
					mos.write( "articleLink", NullWritable.get(), pt );
					mos.write( "articleLink", NullWritable.get(), lang );
					mos.write( "articleLink", NullWritable.get(), val );
				}
				else if( contents.contains( "image" ) && pre.equals( URICenter.predicate_foaf_depiction ) )
					mos.write( "image", NullWritable.get(), val );
				else if( contents.contains( "image" ) && pre.equals( URICenter.predicate_depictionThumbnail ) )
					mos.write( "image", NullWritable.get(), val );
				else if( contents.contains( "image" ) && pre.equals( URICenter.predicate_relatedImage ) )
					mos.write( "image", NullWritable.get(), val );
				else if( contents.contains( "imageInfo" ) && pre.equals( URICenter.predicate_rdfs_label ) && !tr.getSubject().startsWith( "<" + URICenter.domainName ) )
					mos.write( "imageInfo", NullWritable.get(), val );
				else if( contents.contains( "imageInfo" ) && pre.equals( URICenter.predicate_dc_rights ) )
					mos.write( "imageInfo", NullWritable.get(), val );
				else if( contents.contains( "imageInfo" ) && pre.equals( URICenter.predicate_foaf_thumbnail ) )
					mos.write( "imageInfo", NullWritable.get(), val );
				else if( contents.contains( "infoboxText" ) && pre.matches( URICenter.domainName + ".*/property/.*" ) )
					mos.write( "infoboxText", NullWritable.get(), val );
				else if( contents.contains( "exception" ) && pre.equals( URICenter.predicate_temp_exception ) )
					mos.write( "exception", NullWritable.get(), val );
			}
		}
		
		@Override
		public void cleanup( Context context )
		{
			try
			{
				mos.close();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
			catch( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void run( String source, double releaseVersion ) throws Exception
	{
		me.zhishi.tools.Path p = new me.zhishi.tools.Path( releaseVersion, source, true );
		
		String inputPath = p.getRawStructuredDataFolder();
		String outputPath = p.getNTriplesFolder() + source + "/";
		
		Configuration conf = new Configuration();
		
		conf.set( "fs.default.name", me.zhishi.tools.Path.hdfs_fsName );
		FileSystem fs = FileSystem.get( conf );
		fs.delete( new Path( outputPath ), true );
		
		Job job = new Job( conf, "ZHISHI.ME# Sorting NTs by predicate: " + source );
		
		job.setNumReduceTasks( numReduceTasks );

		job.setJarByClass( SortByPredicate.class );
		job.setMapperClass( SortByPredicateMapper.class );
		job.setReducerClass( SortByPredicateReducer.class );
		
		job.setOutputKeyClass( Text.class );
		job.setOutputValueClass( Text.class );
		
		for( String s : contents )
			MultipleOutputs.addNamedOutput( job, s, TextOutputFormat.class, NullWritable.class, Text.class );

		FileInputFormat.addInputPath( job, new Path( inputPath ) );
		FileOutputFormat.setOutputPath( job, new Path( outputPath ) );

		if( job.waitForCompletion( true ) )
		{
			for( String s : contents )
			{
				System.out.println( "Start moveMerging files: " + s );
				SmallTools.moveMergeFiles( fs, s, p.getNTriplesFile( s ), conf, outputPath, numReduceTasks );
			}
			fs.delete( new Path( outputPath ), true );
		}
	}
}
