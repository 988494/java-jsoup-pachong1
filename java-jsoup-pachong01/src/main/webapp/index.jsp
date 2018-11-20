<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>index</title>
    <meta name="description" content="顶呱呱"/>
</head>
<style>
    div{
        margin-top: 20px;
    }
    div > input {
        width: 200px;
        height: 25px;
    }
    button {
        width: 54px;
        height: 30px;
        margin-top: 20px;
    }
    form{
        margin-bottom: 20px;
    }
    span {
        color: red;
    }
</style>
<body>
<form action="crawlWeb" method="get">
    <div>
        <label>要抓取的网址.</label>
        <input type="text" name="rootUrl" placeholder="请输入网址">
    </div>
    <div>
        <label>指定保存地址.</label>
        <input type="text" name="rootDir" placeholder="请输入保存地址">
    </div>
    <button type="submit">确定</button>
</form>
<%

    Object msg = request.getSession().getAttribute("msg");
    if(msg == null){
        msg = "";
    }
    request.getSession().removeAttribute("msg");
%>
<span ><%=msg%></span>
</body>
</html>
