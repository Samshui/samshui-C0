package c0.table;

import c0.Entry.FuncEntry;
import c0.Entry.SymbolEntry;
import c0.error.AnalyzeError;
import c0.error.ErrorCode;
import c0.instruction.Instruction;
import c0.tokenizer.Token;
import c0.tokenizer.TokenType;
import c0.util.MyPair;
import c0.util.Pos;
import c0.util.SymbolType;

import java.util.ArrayList;
import java.util.List;

public class Table {
	private List<FuncEntry> funcTable;
	private List<SymbolEntry> globalSymTable;
	private List<Instruction> globalInstructions;

	/**
	 * 构造函数
	 */
	public Table() {
		this.funcTable = new ArrayList<>();
		this.globalSymTable = new ArrayList<>();
		this.globalInstructions = new ArrayList<>();
	}

	/**
	 * 添加函数
	 *
	 * @param name
	 * @param type
	 * @param pos 报错用
	 */
	public void addFuncEntry(String name, Pos pos) throws AnalyzeError {
		// 查函数表防止重复定义
		if (this.funcExist(name) != null)
			throw new AnalyzeError(ErrorCode.DuplicateFuncName, pos);
		this.funcTable.add(new FuncEntry(name));
	}

	/**
	 * 设置函数属性
	 *
	 * @param type
	 */
	public void addFuncType(TokenType type) {
		FuncEntry getFunc = this.funcTable.get(this.funcTable.size() - 1);
		if (type != TokenType.VOID_TY) {
			getFunc.pushTypeSlot(type);
		}
		getFunc.setFuncType(type);
	}

	/**
	 * 设置全局符号（函数符号）的值类型
	 *
	 * @param name
	 * @param nameToken
	 * @throws AnalyzeError
	 */
	public void addGlobalType(Token nameToken, TokenType type) throws AnalyzeError {
		SymbolEntry symbolEntry = symExist((String) nameToken.getValue());
		if (symbolEntry != null) symbolEntry.setType(type);
		else {
			System.out.println("add global symbol type error");
			throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
		}
	}

	/**
	 * 添加函数参数
	 *
	 * @param name
	 * @param paramToken
	 * @param currentPos
	 * @throws AnalyzeError
	 */
	public void addParam(String name, Token paramToken, Pos currentPos) throws AnalyzeError {
		this.funcTable
				.get(this.funcTable.size() - 1)
				.addParam(name, paramToken.getTokenType(), currentPos);
	}

	/**
	 * 添加全局符号
	 *
	 * @param name
	 * @param type
	 * @param symbolType
	 * @param offset
	 * @param deep
	 * @param value
	 * @param isConstant
	 * @param isInitialized
	 * @throws AnalyzeError
	 */
	public void addGlobalSymbol(String name,
								TokenType type, SymbolType symbolType,
								int deep, Pos currentPos,
								boolean isConstant, boolean isInitialized) throws AnalyzeError {
		if (symExist(name) != null && symExist(name).getType() != TokenType.STRING_LITERAL) {
			throw new AnalyzeError(ErrorCode.DuplicateGlobalVar, currentPos);
		}
		this.globalSymTable.add(new SymbolEntry(name, type, symbolType, deep, (long) this.globalSymTable.size(), isConstant, isInitialized));
	}

	/**
	 * 添加函数参数
	 *
	 * @param name
	 * @param type
	 * @param pos
	 * @throws AnalyzeError
	 */
	public void addParam(String name, TokenType type, Pos pos) throws AnalyzeError {
		this.funcTable
				.get(this.funcTable.size() - 1)
				.addParam(name, type, pos);
	}

	/**
	 * 添加函数实体中的局部符号表
	 *
	 * @param name
	 * @param type
	 * @param symbolType
	 * @param deep
	 * @param isConstant
	 * @param isInitialized
	 * @param currentPos
	 * @throws AnalyzeError
	 */
	public void addFuncSymbol(String name,
							  TokenType type, SymbolType symbolType,
							  int deep,
							  boolean isConstant, boolean isInitialized,
							  Pos currentPos) throws AnalyzeError {
		this.funcTable.get(this.funcTable.size() - 1).addSymbol(name, type, symbolType, deep, isConstant, isInitialized, currentPos);
	}

	/* 搜索类 */

	/**
	 * 搜索该名称函数是否存在
	 *
	 * @param name
	 * @return
	 */
	private FuncEntry funcExist(String name) {
		for (FuncEntry f : this.funcTable) {
			if (f.getFuncName().equals(name)) return f;
		}
		return null;
	}

	/**
	 * 搜索该名称符号是否存在全局中
	 *
	 * @param name
	 * @return
	 */
	private SymbolEntry symExist(String name) {
		for (SymbolEntry s : this.globalSymTable) {
			if (s.getName().equals(name)) return s;
		}
		return null;
	}


	/**
	 * 在table的函数表中查找函数
	 *
	 * @param name
	 * @return
	 */
	public FuncEntry searchFuncInTable(String name) {
		for (FuncEntry f : this.funcTable) {
			if (f.getFuncName().equals(name)) return f;
		}
		return null;
	}

	/**
	 * 获取函数编号
	 *
	 * @param name
	 * @return
	 */
	public int getFuncID(String name) {
		for (int i = 0; i < this.funcTable.size(); i++)
			if (this.funcTable.get(i).getFuncName().equals(name))
				return i + 1;
		return 0;
	}

	/**
	 * 针对一个符号从当前函数查到全局符号表
	 *
	 * @param name
	 * @param deep
	 * @param pos
	 * @return
	 * @throws AnalyzeError
	 */
	public MyPair searchOneSymbolFromLocalToGlobal(String name, int deep, Pos pos) throws AnalyzeError {
		Boolean isGlobal = new Boolean(false);
		Object getSymbol = new Object();

		if (deep > 1) {
			getSymbol = this.funcTable.get(this.funcTable.size() - 1).searchSymbolByDeepIterate(name, deep, pos);
			if (getSymbol == null) {
				// 局部搜索不到该符号，转去全局搜索
				getSymbol = symExist(name);
			}
		} else {
			// 全局赋值语句会用到
			isGlobal = Boolean.valueOf(true);
			getSymbol = symExist(name);
		}
		return new MyPair(getSymbol, isGlobal);
	}


	/**
	 * 查找全局变量（偏移）
	 *
	 * @param name
	 * @return
	 */
	public SymbolEntry getGlobalVar(String name) {
		int off = 0;
		for (SymbolEntry s: this.globalSymTable) {
			// 跳过时增加偏移量
			off++;

			if (s.getName().equals(name)) {
				s.setOff(off);
				return s;
			}
		}
		return null;
	}

	/**
	 * 查找局部变量/形参（偏移）
	 *
	 * @param name
	 * @return
	 */
	public SymbolEntry getFuncVarOrParam(String name) {
		int off = -1;
		boolean getParam = true;
		FuncEntry funcEntry = this.funcTable.get(this.funcTable.size() - 1);

		// todo 这里是否需要判断函数实体是否存入符号表
		// 空出一个slot的返回值
		if (symExist(funcEntry.getFuncName()).getType() != TokenType.VOID_TY) off++;
		for (SymbolEntry s:funcEntry.getSymbolTable()) {
			off++;

			// 跳过所有的参数，重新计算var的偏移
			if (getParam && s.getSymbolType() == SymbolType.VAR) {
				off = 0;
				getParam = false;
			}
			if (s.getName().equals((name))) {
				s.setOff(off);
				return s;
			}
		}
		return null;
	}

	public long getGlobalSymbolOff(String name) {
		for (SymbolEntry s:this.globalSymTable) {
			if (s.getName().equals(name)) return s.getOff();
		}
		return (long) -1;
	}


	/* 指令类 */

	/**
	 * 给当前函数添加指令集
	 *
	 * @param instructions
	 */
	public void addInstructionsToFunc(List<Instruction> instructions) {
		FuncEntry funcEntry = this.funcTable.get(this.funcTable.size() - 1);
		funcEntry.addAllInstructions(instructions);
	}

	public void addOneInstructionToFunc(Instruction instruction) {
		FuncEntry funcEntry = this.funcTable.get(this.funcTable.size() - 1);
		funcEntry.addOneInstruction(instruction);
	}

	/* setter & getter */
	public List<FuncEntry> getFuncTable() {
		return funcTable;
	}

	public void setFuncTable(List<FuncEntry> funcTable) {
		this.funcTable = funcTable;
	}

	public List<SymbolEntry> getGlobalSymTable() {
		return globalSymTable;
	}

	public void setGlobalSymTable(List<SymbolEntry> globalSymTable) {
		this.globalSymTable = globalSymTable;
	}

	public List<Instruction> getGlobalInstructions() {
		return globalInstructions;
	}

	public void setGlobalInstructions(List<Instruction> globalInstructions) {
		this.globalInstructions = globalInstructions;
	}

	@Override
	public String toString() {
		String tableString = new String("");
		for (SymbolEntry s: globalSymTable) tableString += s.toString();
		tableString += "\n";
		for (FuncEntry f:funcTable) tableString += f.toString();
		return tableString;
	}
}
