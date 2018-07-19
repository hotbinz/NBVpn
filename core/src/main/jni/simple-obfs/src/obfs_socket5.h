/*
 * obfs_http.h - Interfaces of http obfuscating function
 *
 * Copyright (C) 2013 - 2016, Max Lv <max.c.lv@gmail.com>
 *
 * This file is part of the simple-obfs.
 *
 * simple-obfs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * simple-obfs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with simple-obfs; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */

#ifndef OBFS_SOCKET5_H
#define OBFS_SOCKET5_H
#define SOCKS_VERSION 0x05
#define SOCKS_METHOD_NO_AUTHENTICATION_REQUIRED 0x00

#include "obfs.h"
struct socks_client_hello_header {
    uint8_t ver;
    uint8_t nmethods;
};
struct socks_client_hello_method {
    uint8_t method;
};
struct socks_server_hello {
    uint8_t ver;
    uint8_t method;
};
struct BSocksClient_auth_info {
    int auth_type;
    union {
        struct {
            const char *username;
            size_t username_len;
            const char *password;
            size_t password_len;
        } password;
    };
};
static uint8_t hton8 (uint8_t x)
{
    return x;
}
extern obfs_para_t *obfs_socket5;

#endif
