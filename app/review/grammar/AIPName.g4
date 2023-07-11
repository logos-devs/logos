/**
* parent/123/entity/456
*/
grammar AIPName;

path: ancestor* file_name;

ancestor: file_name SEPARATOR;

file_name: STRING;

SEPARATOR: '/';
STRING: ([a-zA-Z~0-9._]) ([a-zA-Z0-9.+_-])*;