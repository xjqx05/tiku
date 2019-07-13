/**
 * 题库外部接口
 * 题库系统供其他应用调用的接口
 *
 * OpenAPI spec version: 1.1.0
 * Contact: czfshine@outlook.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

export interface GrantInfo { 
    /**
     * 应用授权id，从管理平台获取
     */
    appToken: string;
    /**
     * 待授权的用户授权码，可本地自行生成，或者不传，服务器生成
     */
    userToken?: string;
    /**
     * 权限角色
     */
    grantRole: string;
    /**
     * 时效，默认300s
     */
    expiresIn?: string;
}