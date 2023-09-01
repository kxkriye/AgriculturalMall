package com.djm.gulimall.gulimallsearch.controller;

import com.djm.gulimall.gulimallsearch.Service.MallSearchService;
import com.djm.gulimall.gulimallsearch.vo.SearchParam;
import com.djm.gulimall.gulimallsearch.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author djm
 * @create 2022-01-27 22:44
 */
@Controller
public class SearchController {
    @Autowired
    MallSearchService mallSearchService;
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        String queryString = request.getQueryString();
        param.set_queryString(queryString);
        SearchResult searchResult = mallSearchService.search(param);
        System.out.println("结果封装"+searchResult.toString());
        model.addAttribute("result", searchResult);
        return "list";
    }
}
