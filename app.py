from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from FlagEmbedding import FlagReranker

app = FastAPI()

# ===== 模型加载（全局单例）=====
reranker = FlagReranker(
    "/Users/vanilo/Downloads/bge-reranker-v2-m31",
    use_fp16=False  # M芯片必须 False
)

# ===== 请求结构 =====
class RerankRequestItem(BaseModel):
    id: str
    content: str
    category: str = ""
    source: str = ""

class RerankRequest(BaseModel):
    query: str
    candidates: List[RerankRequestItem]

class RerankResponseItem(BaseModel):
    id: str
    score: float


# ===== 核心接口 =====
@app.post("/rerank")
def rerank(req: RerankRequest):
    pairs = [(req.query, item.content) for item in req.candidates]

    scores = reranker.compute_score(pairs)

    result = []
    for i, item in enumerate(req.candidates):
        result.append({
            "id": item.id,
            "score": float(scores[i])
        })

    return result


@app.get("/")
def health():
    return {"status": "ok"}