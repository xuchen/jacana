	
	function showTableAlign(align) {
			// split the source and target sentences into words
		var whitespacePattern = /\s/;
		var sourceWords = viewTransposed ? align.target.split(whitespacePattern): align.source.split(whitespacePattern);
		var targetWords = viewTransposed ? align.source.split(whitespacePattern): align.target.split(whitespacePattern);
	
		var width = sourceWords.length;
		var height = targetWords.length;
		
		// initialize the sureGrid and the probGrid
		var sureGrid = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align.sureAlign): align.sureAlign);
		var probGrid = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align.possibleAlign): align.possibleAlign);

		// initialize the highlighted rows and columns
		var sourceHighlights = initalizeBooleanArray(width, "");
		var targetHighlights =  initalizeBooleanArray(height, "");

		var html = "<h4>"+align.name+"</h4><br>"
		html += writeHtmlAlignmentTable(sourceWords, targetWords, sureGrid, probGrid, sourceHighlights, targetHighlights);
		
		$('#tables').html(html);
		return true;	
	}
	
	function showComparisonTableAlign(align1, align2) {
		
		var whitespacePattern = /\s/;
		var transposeAlign2 = false;
		if (align1.target.split(whitespacePattern).length == align2.target.split(whitespacePattern).length &&
				align1.source.split(whitespacePattern).length == align2.source.split(whitespacePattern).length) {
			transposeAlign2 = false;
		} else if (align1.target.split(whitespacePattern).length == align2.source.split(whitespacePattern).length &&
				align1.source.split(whitespacePattern).length == align2.target.split(whitespacePattern).length) {
			transposeAlign2 = true;
			align2.sureAlign = transposeAlignments(align2.sureAlign);
			align2.possibleAlign = transposeAlignments(align2.possibleAlign);
		} else {
			alert("two alignments don't match!"+align1.source+" != "+align2.source)
			return true;
		}
			
		// split the source and target sentences into words
		var sourceWords = viewTransposed ? align1.target.split(whitespacePattern): align1.source.split(whitespacePattern);
		var targetWords = viewTransposed ? align1.source.split(whitespacePattern): align1.target.split(whitespacePattern);
	
		var width = sourceWords.length;
		var height = targetWords.length;
		
		// initialize the sureGrid and the probGrid
		var sureGrid1 = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align1.sureAlign): align1.sureAlign);
		var probGrid1 = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align1.possibleAlign): align1.possibleAlign);
		
		var sureGrid2 = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align2.sureAlign): align2.sureAlign);
		var probGrid2 = initalizeGrid(width, height, viewTransposed ? transposeAlignments(align2.possibleAlign): align2.possibleAlign);
		
		var sureGrid = mergeGrid(sureGrid1, sureGrid2)
		var probGrid = mergeGrid(probGrid1, probGrid2)

		// initialize the highlighted rows and columns
		var sourceHighlights = initalizeBooleanArray(width, "");
		var targetHighlights =  initalizeBooleanArray(height, "");

		var html = "<h4>"+align1.name+"</h4>";
		
		var legendTable = "Legend: <table>\n";
		legendTable += "<tr>\n";
		legendTable += '<td class="black" width=20 height=20/>\n'
		legendTable += '<td>both</td>\n'
		legendTable += '<td class="red" width=20 height=20/>\n'
		legendTable += '<td>only in file1</td>\n'
		legendTable += '<td class="green" width=20 height=20/>\n'
		legendTable += '<td>only in file2</td>\n'
		legendTable += "</tr>\n";
		legendTable += "</table>\n<br>\n";
			
		html += legendTable
		
		html += writeHtmlAlignmentTable(sourceWords, targetWords, sureGrid, probGrid, sourceHighlights, targetHighlights);
		
		$('#tables').html(html);
		return true;	
	}
	
	// merge two grids with the following values when an alignment is in:
	// both: 1, grid1: 2, grid2: 3
	function mergeGrid(grid1, grid2) {
		var grid = new Array(grid1.length);
		for (i = 0; i < grid.length; i++) {
			grid[i] = new Array(grid1[i].length);
			for(j = 0; j < grid1[i].length; j++) {
				grid[i][j] = 0;
			}
		}
		for (i = 0; i < grid.length; i++) {
			for(j = 0; j < grid[i].length; j++) {
				if (grid1[i][j] == 1 && grid2[i][j] == 1)
					grid[i][j] = 1;
				else if (grid1[i][j] == 1 && grid2[i][j] == 0)
					grid[i][j] = 2;
				else if (grid1[i][j] == 0 && grid2[i][j] == 1)
					grid[i][j] = 3;
			}
		}
		return grid;
	}

	// This method outputs the HTML table with clickable grid squares 
	// that are indexed into the sure and prob alignment boolean grids.
	// There is an alternate version for source languages that should be
	// displayed right-to-left.
	function writeHtmlAlignmentTable(sourceWords, targetWords, sureGrid, probGrid, 
					 highlightedSourceWords, highlightedTargetWords) {
		// TODO: scrolling table at http://jsfiddle.net/jhfrench/eNP2N/
		var smallerFont = false;
		var size = 20;
		var fontSize = 0;
		if(sourceWords.length > 40 || targetWords.length > 40) {
			size = 15;
			smallerFont = true;
			fontSize = 3;
		}
		
		var html = "";
			
		html += '<br>\n';
		html += '<br>\n';
		
		html += '<table>\n';
		// print the source words as a table header
		html += '<tr>\n';
		
		// write the source words
		html += '\t<td class="first-column"></td>\n';
		for(i = 0; i < sourceWords.length; i++) {
			var word = sourceWords[i];
			html += '\t<td valign="bottom" align="center">';
			html += '<div style="transform: rotate(-30deg); transform-origin: left bottom; \
                    -webkit-transform: rotate(-30deg); -webkit-transform-origin: left bottom; \
				    -moz-transform: rotate(-30deg); -moz-transform-origin: left bottom;  \
				    -ms-transform: rotate(-30deg); -ms-transform-origin: left bottom;  \
                    z-index:inherit; float:center">';
			if(smallerFont) { 
				html += '<font size=' + fontSize + '>';
			}
			html += word;
			if(smallerFont) html += '</font>';
			
			html += '</div>';
			html += '</td>\n';
		}
		html += '\t<td></td>\n';
		html += '</tr>\n';

		for(row = 0; row < targetWords.length; row++) {
			// print the target word
			html += '<tr>\n';	
			var targetWord = targetWords[row];
			if(!targetIsRTL) {
				html += '<td class="first-column">';
			} else {
				html += '<td class="first-column" dir="rtl">';
			}
			if(smallerFont) { 
				html += '<font size=' + fontSize + '>';
			}
			html += '<span class="blacklink">';
			html += targetWord;
			if(smallerFont) html += '</font>';
			html += '</td>\n\t';
			// print this row
			for(column = 0; column < sourceWords.length; column++) {
				if(sureGrid[column][row] == 1) {
					html += '<td class="black" id="button.' + column + '.' + row + '">';
				} else if(probGrid[column][row] == 1) {
					html += '<td class="gray" id="button.' + column + '.' + row + '">';
				} else if(sureGrid[column][row] == 2) {
					html += '<td class="red" id="button.' + column + '.' + row + '">';
				} else if(probGrid[column][row] == 2) {
					html += '<td class="gray" id="button.' + column + '.' + row + '">';
				} else if(sureGrid[column][row] == 3) {
					html += '<td class="green" id="button.' + column + '.' + row + '">';
				} else if(probGrid[column][row] == 3) {
					html += '<td class="gray" id="button.' + column + '.' + row + '">';
				} else {
					if(highlightedSourceWords[column] || highlightedTargetWords[row]) {
						html += '<td class="highlight" id="button.' + column + '.' + row + '">';
					} else {
						html += '<td class="white" id="button.' + column + '.' + row + '">';
					}
				}
				html += '</td>\n';	
			}
			
			// print the target word again
//			if(!targetIsRTL) {
//				html += '<td>';	
//			} else {
//				html += '<td dir="rtl">';	
//			}
//			if(smallerFont) html += '<font size=' + fontSize + '>';
//			html += '<span class="blacklink">';
//			html += targetWord;
//			if(smallerFont) html += '</font>';
//			html += '</td>\n\t';
			
			html += '</tr>';	
			html += '\n';
		}
		

		// write the source words again
		html += '\t<td class="last-row"></td>\n';
		for(i = 0; i < sourceWords.length; i++) {
			var word = sourceWords[i];
			html += '\t<td valign="top" align="center" class="last-row">';
			html += '<div style="transform: rotate(-30deg); transform-origin: right bottom; \
                    -webkit-transform: rotate(-30deg); -webkit-transform-origin: right bottom; \
				    -moz-transform: rotate(-30deg); -moz-transform-origin: right bottom; \
				    -ms-transform: rotate(-30deg); -ms-transform-origin: right bottom; \
                    z-index:inherit; float:center">';
			if(smallerFont) {
				html += '<font size=' + fontSize + '>';
			}
			html += word;
			if(smallerFont) html += '</font>';
			
			html += '</td>\n';
		}
		html += '\t<td class="last-row"></td>\n';
		html += '</tr>\n';

		html += '</table>\n';
		
		return html;
	}

	// Transposes the string form of the alignment.  Changes each x-y into y-x
	function transposeAlignments(alignmentString) {
		var transposedAlignmentsString = "";
		var whitespacePattern = /\s/;
		var dash = '-';
		var points = alignmentString.split(whitespacePattern);
		for(i = 0; i < points.length; i++) {
			if(points[i].indexOf(dash) > 0) {
				var point = points[i].split(dash);
				var x = point[0];
				var y = point[1];
				var transposedAlignmentsString = transposedAlignmentsString + y + "-" + x + " ";
			}
		}	
		transposedAlignmentsString.replace(/\s$/, '');
		return transposedAlignmentsString;
	}

	
	// Returns an initialized boolean array
	function initalizeBooleanArray(length, indexOfTruesString) {
		// pad the indexOfTruesString with spaces
		indexOfTruesString = " " + indexOfTruesString + " ";
		var array = new Array(length);
		for (i = 0; i < array.length; i++) {
			array[i] = false;
		}

		// set the points in alignmentString to true
		var whitespacePattern = /\s/;
		var indicies = indexOfTruesString.split(whitespacePattern);
		for(i = 0; i < indicies.length; i++) {
			var index = indicies[i];
			array[index] = true;
		}	
		return array;
	}
	
	// Returns an initialized grid.  Sets the points to 1
	// that are included in the alignmentString as "x-y".
	function initalizeGrid(width, height, alignmentString) {
		var grid = new Array(width);
		for (i = 0; i < grid.length; i++) {
			grid[i] = new Array(height);
			for(j = 0; j < height; j++) {
				grid[i][j] = 0;
			}
		}
		// Set the points in alignmentString to true
		var whitespacePattern = /\s/;
		var dash = '-';
		var points = alignmentString.split(whitespacePattern);
		for(i = 0; i < points.length; i++) {
			if(points[i].indexOf(dash) > 0) {
				var point = points[i].split(dash);
				var x = point[0];
				var y = point[1];
				grid[x][y] = 1;
			}
		}	
		return grid;
	}
