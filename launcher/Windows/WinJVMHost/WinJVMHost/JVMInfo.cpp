#include "stdafx.h"
#include "JVMInfo.h"

#ifndef UNICODE
#define UNICODE
#endif 
#ifdef _M_X64
bool need64BitJRE = true;
#define BITS_STR L"64-bit"
#else
bool need64BitJRE = false;
#define BITS_STR L"32-bit"
#endif


using namespace std;

/* Ctors */

JVMInfo::JVMInfo() {
	rootPath = L"";
	dllPath = L"";	
	found = LocateJVM();
}

bool JVMInfo::IsFound()
{
	return found;
}

bool JVMInfo::Is64Bit()
{
	wstring cfgPath(rootPath);
	return IsValidPath(rootPath.append(L"\\lib\\amd64\\jvm.cfg"));
	
}

wstring JVMInfo::RootPath()
{
	return rootPath;
}

wstring JVMInfo::DllPath()
{
	return dllPath;
}


/* Private members*/

bool JVMInfo::FindFromEnv(wstring path)
{
	return false;
}

bool JVMInfo::IsValidPath(wstring path) {
	return GetFileAttributesW(path.c_str()) != INVALID_FILE_ATTRIBUTES;
}


bool JVMInfo::ContainValidJVM(wstring path)
{
	wstring _rootPath(path);
	if (_rootPath[_rootPath.size() - 1] != L'\\')
	{
		_rootPath.append(L"\\");
	}

	vector<wstring> candidates{
		L"bin\\server\\jvm.dll",
		L"bin\\client\\jvm.dll",
		L"jre\\bin\\server\\jvm.dll",
		L"jre\\bin\\client\\jvm.dll" };

	for (vector<wstring>::iterator it = candidates.begin(); it != candidates.end(); ++it) {
		wstring _dllPath(_rootPath);
		if (IsValidPath(_dllPath.append(*it))) {
			this->rootPath = _rootPath;
			this->dllPath = _dllPath;

			if (this->Is64Bit() == need64BitJRE) {
				return true;
			}
			else 
			{
				L_(lerror) << L"The JVM found at " << rootPath << " is " << (Is64Bit() ? L"64 bits" : L"32 bits") << L" but the host process is " << BITS_STR ;
			}		
		}
	}
	return false;
}


bool JVMInfo::LocateFromEnv(wstring envVarName)
{
	wchar_t envVarValue[_MAX_PATH];
	if (GetEnvironmentVariableW(envVarName.c_str(), envVarValue, _MAX_PATH - 1))
	{
		if (ContainValidJVM(envVarValue))
		{
			L_(linfo) << L"JVM located via environment variable.";
			return true;
		}
		else
		{
			L_(lerror) << L"Failed to find a suitable installation of the JVM from the environment variable " << envVarName << L"=" << envVarValue;
		}
	}
	else
	{
		L_(lerror) << L"Environment variable " << envVarName << L"is not defined";
	}
	return false;
}

bool JVMInfo::LocateJVM()
{
	if (LocateFromEnv(L"JAVA_HOME"))
	{
		return true;
	}

	//if (FindJVMInSettings()) return true;

	//std::vector<std::string> jrePaths;
	//if (need64BitJRE) jrePaths.push_back(GetAdjacentDir("jre64"));
	//jrePaths.push_back(GetAdjacentDir("jre"));
	//for (std::vector<std::string>::iterator it = jrePaths.begin(); it != jrePaths.end(); ++it) {
	//	if (FindValidJVM((*it).c_str()) && Is64BitJRE(jvmPath) == need64BitJRE)
	//	{
	//		return true;
	//	}
	//}



	///*if (FindJVMInRegistry())
	//{
	//	return true;
	//}*/

	//std::string jvmError;
	//jvmError = "No JVM installation found. Please install a " BITS_STR " JDK.\n"
	//	"If you already have a JDK installed, define a JAVA_HOME variable in\n"
	//	"Computer > System Properties > System Settings > Environment Variables.";



	//std::string error = LoadStdString(IDS_ERROR_LAUNCHING_APP);
	//MessageBoxA(NULL, jvmError.c_str(), error.c_str(), MB_OK);
	return false;
}