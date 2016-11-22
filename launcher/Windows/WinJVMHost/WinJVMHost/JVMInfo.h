#pragma once

#ifndef UNICODE
#define UNICODE
#endif 
#include <string>

using namespace std;


class JVMInfo {
public:
	JVMInfo();

	bool IsFound();

	bool Is64Bit();

	wstring RootPath();

	wstring DllPath();


private:
	bool found, is64Bit;

	wstring rootPath, dllPath;

	bool FindFromEnv(wstring path);

	bool IsValidPath(wstring path);

	bool ContainValidJVM(wstring path);

	bool LocateFromEnv(wstring envVarName);

	bool LocateJVM();

};