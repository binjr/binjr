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

	wstring RootPath();

	wstring DllPath();


private:
	bool found;

	wstring rootPath, dllPath;

	bool FindFromEnv(wstring path);

	bool IsValidPath(wstring path);

	bool FindValidJVM(const wchar_t * path);

	bool LocateFromEnv(wstring envVarName, bool & result);

	bool LocateJVM();

};