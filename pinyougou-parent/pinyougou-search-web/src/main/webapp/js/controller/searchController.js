app.controller('searchController',function($scope,$location,searchService){

	$scope.search=function(){
	    $scope.searchMap.pageNo=parseInt($scope.searchMap.pageNo)
		searchService.search($scope.searchMap).success(
			function(response){
				$scope.resultMap=response;
				buildPageLabel();//调用分页
			}
		);		
	}

	$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''}
	//添加搜索选项
	$scope.addSearchItem=function (key,value) {
		//如果是分类和品牌，因为分类和品牌是不变的值
		if(key=='category'||key=='brand'||key=='price'){
			$scope.searchMap[key]=value;
		}else { //如果是规格
			$scope.searchMap.spec[key]=value;

		}
		$scope.search();
    }

    //移除复合搜索条件
	$scope.removeSearchItem=function (key) {
		if(key=='category'||key=='brand'||key=='price'){
			$scope.searchMap[key]="";
		}else{
			delete $scope.searchMap.spec[key];//根据键删除该键值对。
		}
        $scope.search();//执行搜索
    }

    //构建分页标签（totalPages为总页数）
    buildPageLabel=function () {
        $scope.pageLabel = [];//新增分页栏数属性
        var maxPageNo = $scope.resultMap.totalPages;//得到最后页码
        var firstPage = 1;//开始页码
        var lastPage = maxPageNo;
        $scope.firstDot = true;//前面有点
        $scope.lastDot = true;//后面有点
        if ($scope.resultMap.totalPages > 5) { //如果总页码大于5页，显示部分页码
            if ($scope.searchMap.pageNo < 3) { //如果当前页码小于3，显示前5页
                lastPage = 5;
                $scope.firstDot = false;//前面没有点
            } else if ($scope.searchMap.pageNo >= lastPage - 2) { //如果当前页码大于等于最大页码-2
                firstPage = maxPageNo - 4;
                $scope.lastDot = false;//后面没有点
            } else {
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        } else {
            $scope.firstDot = false;//前面没点
            $scope.lastDot = false;//后面没点
        }
        //循环产生页码标签
        for (var i = firstPage; i <= lastPage; i++) {
            $scope.pageLabel.push(i);
        }
    }
        //根据页码执行查询
        $scope.queryByPage = function (pageNo) {
            //页码验证
            if (pageNo < 1 || pageNo > $scope.resultMap.totalPages) {
                return;
            } else {
                $scope.searchMap.pageNo = pageNo;
                $scope.search();
            }
        }
        
        //设置页码不可用样式
    //判断当前页为第一页
    $scope.isTopPage=function(){
        if($scope.searchMap.pageNo==1){
            return true;
        }else{
            return false;
        }
    }
    //判断当前页是否为最后一页
    $scope.isEndPage=function(){
        if($scope.searchMap.pageNo==$scope.resultMap.totalPages){
            return true;
        }else{
            return false;
        }
    }

    //设置排序规则
    $scope.sortSearch=function (sort,sortField) {
        $scope.searchMap.sort=sort;
        $scope.searchMap.sortField=sortField;
        $scope.search();
    }

    //判断关键字是不是品牌
    $scope.keywordsIsBrand=function () {
        for(var i=0;i<$scope.resultMap.brandList.length;i++){
            if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>=0){
                //如果包含
                return true;
            }

            }
        return false;
    }

    //加载关键字
    $scope.loadkeywords=function () {
       $scope.searchMap.keywords=$location.search()['keywords'] ;
       $scope.search();
    }
});