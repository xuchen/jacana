#!/usr/bin/env perl

if(@ARGV != 2){
	die "$0 : <weka output> <QA data file>\n";
}

open(WEKA, $ARGV[0]);
open(QA, $ARGV[1]);

$resline = "";

$inst = 0;
$inoutput = 0;
$querynum = 1;
$wekaline = <WEKA>;

$buffer = "";
$hasPositive = 0;
$hasNegative = 0;

while($wekaline){
	if($wekaline =~ m/inst#/){
		$inoutput = 1;
		$wekaline = <WEKA>;
	}
	if($inoutput != 1 || $wekaline =~ m/^\s*$/){
		$wekaline = <WEKA>;
		next;
	}

	while($line = <QA>){
		if($line =~ m/<\/[a-z]+tive>/){
			last;
		}
		if($line =~ m/<positive>/){
			$explabel = "positive";
			$hasPositive=1;
		}elsif($line =~ m/<negative>/){
			$explabel = "negative";
			$hasNegative=1;
		}elsif($line =~ m/<\/QApairs/){
			if($hasPositive ==1 && $hasNegative == 1){
				print $buffer;
			}
			$hasPositive=0;
			$hasNegative=0;
			$buffer = "";
			$querynum++;
			$inst = 0;
		}
	}

	chomp($wekaline);
	$wekaline =~ s/\+//;
	$wekaline =~ s/^\s+//;
	@wekaparts = split(/\s+/, $wekaline);

	if($wekaparts[1] =~ m/:n/){
		$label = "negative";
	}else{
		$label = "positive";
	}
	if($wekaparts[2] =~ m/:n/){
		$score = 1.0-$wekaparts[3];
	}else{
		$score = $wekaparts[3];
	}

	if($label ne $explabel){
		print STDERR "ERROR: weka output says $label but file says $explabel\n";
	}
	#print STDERR "weka score $inst: $score\n";
	$buffer .= "$querynum Q0 $inst 0 $score WEKA:$label-FILE:$explabel\n";	
	
	$inst++;
	$wekaline = <WEKA>;
}

if($hasPositive ==1 && $hasNegative == 1){
	print $buffer;
}

while(<QA>){
	print STDERR "EXTRA INPUT:$_";
}
