import os
import sys
import subprocess
from google import genai

def get_git_diff():
    base = os.environ.get('BASE_BRANCH')
    # If there's no base branch (e.g. first push) or it's a push event missing the previous sha
    if not base or base == '0000000000000000000000000000000000000000':
        try:
            return subprocess.check_output(['git', 'diff', 'HEAD~1', 'HEAD'], text=True)
        except subprocess.CalledProcessError:
            return ""
    try:
        return subprocess.check_output(['git', 'diff', base, 'HEAD'], text=True)
    except subprocess.CalledProcessError:
        return ""

def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Error: GEMINI_API_KEY secret is not set in GitHub Settings > Secrets.")
        sys.exit(1)

    diff = get_git_diff()
    if not diff.strip():
        print("No code changes to review.")
        sys.exit(0)

    try:
        with open('AGENTS.md', 'r', encoding='utf-8') as f:
            rules = f.read()
    except FileNotFoundError:
        rules = "AGENTS.md not found. Proceed with standard KMP best practices."

    client = genai.Client(api_key=api_key)

    prompt = f"""
    Sen Good4 projesi için mimari ve kod standartlarını denetleyen bir AI Code Reviewer'sın.
    Aşağıda projenin mimari kuralları (AGENTS.md) ve diğer bir developer tarafından yapılan son kod değişikliklerinin Git Diff'i yer almaktadır.
    
    KURALLAR:
    {rules}
    
    DEĞİŞİKLİKLER (Git Diff):
    {diff}
    
    GÖREV:
    Bu değişiklikleri yukarıdaki kurallara göre çok sıkı bir şekilde incele.
    Eğer kuralları ihlal eden en ufak bir durum varsa (ViewModel yerine UI'da logic/state tutulması, yanlış isimlendirme, hardcoded text/renk, koin eksikliği vb.):
    1. İhlalleri tek tek, dosya ismiyle beraber Türkçe açıkla ve düzeltme önerisi ver.
    2. Cevabının en son satırına tam olarak 'KARAR: REJECTED' yaz.
    
    Eğer kod kurallara tamamen uygunsa:
    1. İncelemenin kısa özetini yaz.
    2. Cevabının en son satırına tam olarak 'KARAR: APPROVED' yaz.
    """

    print("Analyzing code changes with Gemini 2.5 Pro...\n")
    response = client.models.generate_content(
        model='gemini-2.5-pro',
        contents=prompt
    )
    
    print("=== AI REVIEW REPORT ===")
    print(response.text)
    print("========================\n")

    if "KARAR: REJECTED" in response.text:
        print("\n❌ AI Review Failed: Kod standartlara uymadığı için reddedildi.")
        sys.exit(1) # This fails the GitHub Action build
    else:
        print("\n✅ AI Review Passed: Değişiklikler kurallara uygun.")
        sys.exit(0)

if __name__ == "__main__":
    main()
