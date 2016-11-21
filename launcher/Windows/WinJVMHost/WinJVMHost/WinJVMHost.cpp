#include "stdafx.h"
#include "JVMInfo.h"

#ifndef UNICODE
#define UNICODE
#endif 

using namespace std;
HINSTANCE hInst; // Current instance.




//
//bool FindJVMInSettings() {
//	TCHAR buffer[_MAX_PATH];
//	TCHAR copy[_MAX_PATH];
//
//	GetModuleFileName(NULL, buffer, _MAX_PATH);
//	std::wstring module(buffer);
//
//	if (LoadString(hInst, IDS_VM_OPTIONS_PATH, buffer, _MAX_PATH)) {
//		ExpandEnvironmentStrings(buffer, copy, _MAX_PATH - 1);
//		std::wstring path(copy);
//		path += L"\\config" + module.substr(module.find_last_of('\\')) + L".jdk";
//		FILE *f = _tfopen(path.c_str(), _T("rt"));
//		if (!f) return false;
//
//		char line[_MAX_PATH];
//		if (!fgets(line, _MAX_PATH, f)) {
//			fclose(f);
//			return false;
//		}
//
//		TrimLine(line);
//		fclose(f);
//
//		return FindValidJVM(line);
//	}
//	return false;
//}


int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE, PWSTR pCmdLine, int nCmdShow)
{
	initLogger(L"log.txt", ldebug);

	L_(linfo) << L"Starting";
	

	hInst = hInstance;
	JVMInfo *jvmInfo = new JVMInfo();

	if (jvmInfo->IsFound()) {
		std::wostringstream msg;
		msg << L"Found jvm: \n - " << jvmInfo->RootPath() << L"\n - " << jvmInfo->DllPath();
		MessageBoxW(NULL, msg.str().c_str(), L"Success", MB_OK);
	}


	endLogger();

	return 0;
}


