package org.juxtalearn.rias.components.visualizationtoserver;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;

public class ClosureMinifier {
	private final Reader input;
	
	public ClosureMinifier(Reader input) {
		this.input = input;
	}
	
	public String minify() throws IOException {
		List<JSSourceFile> externs = Collections.emptyList();
		List<JSSourceFile> inputs = Arrays.asList(JSSourceFile.fromCode("default.js", getCodeFromReader()));
		
		CompilerOptions options = new CompilerOptions();
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
		
		com.google.javascript.jscomp.Compiler compiler = new Compiler();
		Result result = compiler.compile(externs, inputs, options);
		
		if(result.success) {
			return compiler.toSource();
			//return new StringReader(compiler.toSource());
		}
		
		throw new IllegalArgumentException("Unable to minify input source");
	}
	
	public String getCodeFromReader() throws IOException {
		StringBuilder sb = new StringBuilder();
		
		int c;
		while((c = this.input.read()) != -1) {
			sb.append((char)c);
		}
		return sb.toString();
	}
}
