package c0;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import c0.analyser.Analyser;
import c0.error.CompileError;
import c0.instruction.Instruction;
import c0.table.Table;
import c0.tokenizer.StringIter;
import c0.tokenizer.Token;
import c0.tokenizer.TokenType;
import c0.tokenizer.Tokenizer;

import c0.util.OutPutBinary;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
	public static void main(String[] args) throws CompileError {
		var argparse = buildArgparse();
		Namespace result;
		try {
			result = argparse.parseArgs(args);
		} catch (ArgumentParserException e1) {
			argparse.handleError(e1);
			return;
		}

		var inputFileName = result.getString("input");
		var outputFileName = result.getString("output");

		InputStream input;
		if (inputFileName.equals("-")) {
			input = System.in;
		} else {
			try {
				input = new FileInputStream(inputFileName);
			} catch (FileNotFoundException e) {
				System.err.println("Cannot find input file.");
				e.printStackTrace();
				System.exit(2);
				return;
			}
		}

		PrintStream output;
		if (outputFileName.equals("-")) {
			output = System.out;
		} else {
			try {
				output = new PrintStream(new FileOutputStream(outputFileName));
			} catch (FileNotFoundException e) {
				System.err.println("Cannot open output file.");
				e.printStackTrace();
				System.exit(2);
				return;
			}
		}

		Scanner scanner;
		scanner = new Scanner(input);
		var iter = new StringIter(scanner);
		var tokenizer = tokenize(iter);

		if (result.getBoolean("tokenize")) {
			// tokenize
			var tokens = new ArrayList<Token>();
			try {
				while (true) {
					var token = tokenizer.nextToken();
					if (token.getTokenType().equals(TokenType.EOF)) {
						break;
					}
					tokens.add(token);
				}
			} catch (Exception e) {
				// 遇到错误不输出，直接退出
				System.err.println(e);
				System.exit(1);
				return;
			}
			for (Token token : tokens) {
				System.out.println(token.toString());
			}
		} else if (result.getBoolean("analyse")) {
			// analyze
			var analyzer = new Analyser(tokenizer);
			List<Instruction> instructions;
			try {
				instructions = analyzer.analyse();
				Table table = analyzer.retTable();
				System.out.println(table.toString());

				OutPutBinary outPutBinary = new OutPutBinary(table);
				List<Byte> bytes = outPutBinary.generate();
				byte[] tmp = new byte[bytes.size()];

				for (int i = 0; i < bytes.size(); i++) tmp[i] = bytes.get(i);

				output.write(tmp);
			} catch (Exception e) {
				// 遇到错误不输出，直接退出
				System.err.println(e);
				System.exit(1);
				return;
			}
		} else {
			System.err.println("Please specify either '--analyse' or '--tokenize'.");
			System.exit(3);
		}
	}

	private static ArgumentParser buildArgparse() {
		var builder = ArgumentParsers.newFor("miniplc0-java");
		var parser = builder.build();
		parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
		parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
		parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
				.action(Arguments.store());
		parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
		return parser;
	}

	private static Tokenizer tokenize(StringIter iter) {
		var tokenizer = new Tokenizer(iter);
		return tokenizer;
	}
}
