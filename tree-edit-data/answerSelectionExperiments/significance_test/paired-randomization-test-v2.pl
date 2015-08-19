
#
# Paired Randomization Test Implementation 
# By Mark D. Smucker (C) 2008
# 
# This Perl script does two sided paired randomization tests.
#
# Usage: reads filenames as arguments, all files are
# measured with respect to the first file listed.
# All files should be created in the relational output
# created by Buckley's trec_eval with the -q option
# to produce statistics for all queries.
#
# Output is tab separated columns that should be understandable.
#
# While the code below should be error free, we recommend 
# checking the p-values against the p-values of Student's 
# t-test as a sanity test prior to publication.
#
# Required arguments:
#
# -t test, where "test" is one of:
#     + "g" for geometric mean
#     + "a" for arithmetic mean 
#
# Optional arguments:
#
# -q queryIDsFile, where "queryIDsFile" is a file containing 
# the queryIDs to use.  One queryId per line.  If not specified, 
# all queries are used.
#
# -d desiredMetricsFile, where "desiredMetricsFile is a file
# containing the metrics to use.  One metric name per line.  These
# are the metric names as output by trec_eval.  If not specified, 
# all metrics are used.
#
#--------------------------------------------------------------
#
# License: Script is offered without any warranty.  If you use 
# this script and publish your results, please cite:
#
#      "A Comparison of Statistical Significance Tests for 
#      Information Retrieval Evaluation" by Mark D. Smucker, 
#      James Allan, and Ben Carterette. CIKM 2007.
#
# @inproceedings{Smucker07:StatSig,
#  author = {Mark D. Smucker and James Allan and Ben Carterette},
#  title = {A comparison of statistical significance tests for information retrieval evaluation},
#  booktitle = {CIKM '07: Proceedings of the sixteenth ACM conference on Conference on information and knowledge management},
#  year = {2007},
#  isbn = {978-1-59593-803-9},
#  pages = {623--632},
#  location = {Lisbon, Portugal},
#  doi = {http://doi.acm.org/10.1145/1321440.1321528},
#  publisher = {ACM},
#  address = {New York, NY, USA},
#  }
#
#---------------------------------------------------------------
#
# Created 1-19-2005 by Mark D. Smucker
#
# 12-31-2005 modified by Mark to handle geometric mean
# as well as arithmetic mean
#
# 1-4-2008 cleaned up by Mark and switched to MT RNG
#
#---------------------------------------------------------------

# The Mersenne Twister RNG of Matsumoto and Nishimura.
# http://search.cpan.org/~ams/Math-Random-MT-1.07/MT.pm
use Math::Random::MT ;

$seed = 994158012 ; # for reproducible results
$mtRNG = Math::Random::MT->new( $seed ) ;

use Getopt::Std;

sub Avg
{
    my $aRef = shift ;
    my $i;
    my $sum = 0 ;
    my $avg = 0 ;
    my $numElts = @$aRef ;
    for ($i = 0 ; $i < $numElts ; ++$i ) 
    {
	$sum += $aRef->[$i] ;
    }
    $avg = $sum / $numElts ;
    return $avg ;
}

sub GeoMean
{
    my $aRef = shift ;
    my $i;
    my $sumLogs = 0 ;
    my $avgLogs = 0 ;
    my $geoMean = 0 ;
    my $numElts = @$aRef ;
    my $val = 0 ;
    for ($i = 0 ; $i < $numElts ; ++$i ) 
    {
	$val = $aRef->[$i] ;
	if ( $val < 0.00001 ) # TREC Robust behavior, Vorhees TREC 2005 Robust Overview
	{
	    $val = 0.00001 ;
	}
	$sumLogs += log($val) ;
    }
    $avgLogs = $sumLogs / $numElts ;
    $geoMean = exp($avgLogs) ;
    return $geoMean ;
}

# func(randB) - func(randA) where randB[i] and randA[i] are randomly selected 
# from B[i] and A[i] (elts may be swapped compared to original array at same index).
sub RandomDiff 
{
    my $funcRef = shift ;
    my $aRef = shift ;
    my $bRef = shift ;
    my $i;
    my $numElts = @$aRef ;
    my @randAlpha = () ;
    my @randBeta = () ;
    for ($i = 0 ; $i < $numElts ; ++$i ) 
    {
        my $j = $mtRNG->rand() ;
        if ( $j < 0.5 )
	{
	    push( @randAlpha, $aRef->[$i] ) ;
	    push( @randBeta,  $bRef->[$i] ) ;
	}
	else
	{
	    push( @randAlpha, $bRef->[$i] ) ;
	    push( @randBeta,  $aRef->[$i] ) ;
	}
    }
    my $funcAlpha = $funcRef->( \@randAlpha ) ;
    my $funcBeta =  $funcRef->( \@randBeta ) ;
    my $diff = $funcBeta - $funcAlpha ;
    return $diff ;
}

# a two sided paired permutation test,
# see Cohen pg 168 (Empirical Methods in AI)
sub SigLevel 
{
    my $funcRef = shift ;
    my $aRef = shift ;
    my $bRef = shift ;
    my $funcA = $funcRef->( $aRef ) ;
    my $funcB = $funcRef->( $bRef ) ;
    my $funcDiff = $funcB - $funcA ;
    my $numSamples = 100000 ; # see Efron p 208-211 and the CIKM paper
    my $i ;
    # we could divide the number of ties in half because... well,
    # that is what Box does on page 100 (who is doing a one-sided test), 
    # but Efron does not, page 203,
    # but on page 212 for two sided he doesn't count them at all...
    # we'll be conservative and count them all as Cohen does.
    my $numEqualOrGreater = 0 ;
    for ( $i = 0 ; $i < $numSamples ; ++$i )
    {
	if ( abs(RandomDiff( $funcRef, $aRef, $bRef)) >= abs($funcDiff) )
	{
	    ++$numEqualOrGreater ;
	}
    }
    return $numEqualOrGreater / $numSamples ;
}

# -t ARG sets $opt_t, for "test" which should be:
#     + "g" for geometric mean
#     + "a" for arithmetic mean 
#
getopt("dtq"); 

if ( ! $opt_t )
{
    print "usage: perl -w paired-randomization-test.pl -t a|g trec_eval_b.out1 ... trec_eval_b.outN\n" ;
    print "Need to specify -t with g or a for geometric or arithmetic mean.\n" ;
    print "Please see script for other options.\n" ;
    exit(0) ;
}
elsif ( $opt_t eq "g" )
{
    $func = \&GeoMean ;
}
elsif ( $opt_t eq "a" )
{
    $func = \&Avg ;
}
else
{
    print "Code given to -t was not an a nor a g.\n" ;
    print "usage: perl -w paired-randomization-test.pl -t a|g trec_eval_b.out1 ... trec_eval_b.outN\n" ;
    print "Need to specify -t with g or a for geometric or arithmetic mean.\n" ;
    print "Please see script for other options.\n" ;
    exit(0) ;
}

%queryIDsToUse = () ;
$restrictQueryIDs = 0 ;
if ( $opt_q )
{
    $restrictQueryIDs = 1 ;
    open( QFILE, "<$opt_q" ) ;
    while ( $line = <QFILE> ) 
    {
	chomp($line) ;
	$queryIDsToUse{$line} = 1 ;
    }
    close( QFILE ) ;
}

%desiredMetrics = () ;
$restrictMetrics = 0 ;
if ( $opt_d )
{
    $restrictMetrics = 1 ;
    open( DFILE, "<$opt_d" ) ;
    while ( $line = <DFILE> ) 
    {
	chomp($line) ;
	$desiredMetrics{$line} = 1 ;
    }
    close( DFILE ) ;
}

$numArgs = @ARGV ;
if ( $numArgs < 2 )
{
    print "Error: too few arguments.\n" ;
    print "usage: perl -w paired-randomization-test.pl -t a|g trec_eval_b.out1 ... trec_eval_b.outN\n" ;
    print "Need to specify -t with g or a for geometric or arithmetic mean.\n" ;
    print "Please see script for other options.\n" ;
    exit( 0 ) ;
}
@filenames = @ARGV ;
$numFiles = $numArgs ;
%filename2metric = () ; # filename -> ref hash of metric -> ref hash queryID -> score
%metrics = () ; # used as a set
%queryIDs = () ; # used as a set

foreach $filename (@filenames)
{
    open( IN, "<$filename" ) || die( "unable to open file $filename" ) ;
    while ( $line = <IN> )
    {
	chomp($line) ;
	@fields = split( /\s+/, $line ) ;
	$numFields = @fields ;
	die( "Each file should contain 3 columns.\n" ) if $numFields != 3 ;
	$metric = $fields[0] ;
	$queryID = $fields[1] ;
	$score = $fields[2] ;
	next if ( $queryID eq "all" ) ; # this isn't a query! ignore
	next if ( $restrictQueryIDs && ! exists( $queryIDsToUse{$queryID} ) ) ; 
	next if ( $restrictMetrics && ! exists( $desiredMetrics{$metric} ) ) ;
	$metrics{$metric} = 1 ; 
	$queryIDs{$queryID} = 1 ;

	if ( ! exists($filename2metric{$filename}) )
	{
	    $filename2metric{$filename} = {} ; # anon ref to an empty hash
	}
	$refMetric2QueryID = $filename2metric{$filename} ;
	if ( ! exists($refMetric2QueryID->{$metric}) )
	{
	    $refMetric2QueryID->{$metric} = {} ; # anon ref to an empty hash
	}
	$refQueryID2score = $refMetric2QueryID->{$metric} ;
	$refQueryID2score->{$queryID} = $score ;
	
    }
    close(IN) ;
}

# check the quality of the input data
$errorFound = 0 ;
foreach $filename ( @filenames )
{
    if ( ! exists($filename2metric{$filename}) )
    {
	warn( "$filename does not contain any per query data.  Remember to run trec_eval with -q." ) ;
	$errorFound = 1 ;
    }
    else
    {
	$refMetric2QueryID = $filename2metric{$filename} ;
	foreach $metric ( keys %metrics )
	{
	    if ( ! exists($refMetric2QueryID->{$metric}) )
	    {
		warn( "$filename does not contain the metric $metric." ) ;
		$errorFound = 1 ;
	    }
	    else
	    {
		$refQueryID2score = $refMetric2QueryID->{$metric} ;
		foreach $queryID ( sort keys %queryIDs )
		{
		    if ( ! exists($refQueryID2score->{$queryID}) )
		    {
			warn( "$filename does not contain a score for query $queryID for metric $metric" ) ;
			$errorFound = 1 ;
		    }
		}
	    }
	}
    }
}
if ( $errorFound )
{
    exit(1) ;
}

print "run1name\trun2name\tmetric\trun1score\trun2score\tpctChange\tsigLevel\n" ;
# now do the significance testing...
for ( $i = 1 ; $i < $numFiles ; ++$i )
{
    foreach $metric ( sort keys %metrics )
    {
	$alphaFilename = $filenames[0] ;
	$betaFilename = $filenames[$i] ;

	$refAlphaMetric2QueryID = $filename2metric{$alphaFilename} ;
	$refBetaMetric2QueryID = $filename2metric{$betaFilename} ;

	$refAlphaQueryID2score = $refAlphaMetric2QueryID->{$metric} ;
	$refBetaQueryID2score = $refBetaMetric2QueryID->{$metric} ;

	@alphaScores = () ;
	@betaScores = () ;

	foreach $queryID ( sort keys %queryIDs )
	{
	    push( @alphaScores, $refAlphaQueryID2score->{$queryID} ) ;
	    push( @betaScores, $refBetaQueryID2score->{$queryID} ) ;
	}
#	$diff = FuncDiff( $func, \@betaScores, \@alphaScores ) ; # diff = beta - alpha
#	$avgDiff = AvgDiff( \@betaScores, \@alphaScores ) ; # diff = beta - alpha
	$funcAlpha = $func->( \@alphaScores ) ;
	$funcBeta = $func->( \@betaScores ) ;
	$diff = $funcBeta - $funcAlpha ;
	if ( $funcAlpha != 0 )
	{
	    $pct = 100.0 * $diff / $funcAlpha ;
	}
	else
	{
	    $pct = "undef" ;
	}
	$sigLevel = SigLevel( $func, \@alphaScores, \@betaScores ) ;
	printf "$alphaFilename\t$betaFilename\t$metric\t%.6f\t%.6f\t%.6f\t%.6f\n",
	    $funcAlpha, $funcBeta, $pct,$sigLevel ;
    }
}


#  foreach $filename ( sort keys %filename2metric )
#  {
#      $refMetric2QueryID = $filename2metric{$filename} ;
#      foreach $metric ( sort keys %$refMetric2QueryID )
#      {
#  	$refQueryID2score = $refMetric2QueryID->{$metric} ;
#  	foreach $queryID ( sort keys %$refQueryID2score )
#  	{
#  	    $score = $refQueryID2score->{$queryID} ;
#  	    print "$filename $metric $queryID $score\n" ;

#  	}
#      }
#  }
